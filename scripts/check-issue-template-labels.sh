#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="${ROOT_DIR}/.github/ISSUE_TEMPLATE"

# Ensure jq is available for JSON parsing.
if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required but was not found in PATH."
  exit 1
fi

# Require repository context for label lookup.
if [ -z "${GITHUB_REPOSITORY:-}" ]; then
  echo "GITHUB_REPOSITORY is required (example: owner/repo)."
  exit 1
fi

# Prefer Actions token but allow GH_TOKEN fallback for local runs.
auth_token="${GITHUB_TOKEN:-${GH_TOKEN:-}}"

if [ -z "${auth_token}" ]; then
  echo "GITHUB_TOKEN (or GH_TOKEN) is required."
  exit 1
fi

# Fetch all existing labels from the repository.
existing_labels="$(
  curl -fsSL \
    -H "Authorization: Bearer ${auth_token}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${GITHUB_REPOSITORY}/labels?per_page=100" \
  | jq -r '.[].name'
)"

missing=0

# Validate each issue form template except the shared config.
for template in "${TEMPLATE_DIR}"/*.yml; do
  [ "$(basename "${template}")" = "config.yml" ] && continue

  # Parse the inline labels array from template metadata.
  labels_line="$(grep -E '^labels:' "${template}" || true)"
  if [ -z "${labels_line}" ]; then
    echo "Missing labels field: ${template}"
    missing=1
    continue
  fi

  template_labels="$(printf '%s\n' "${labels_line}" | grep -oE '"[^"]+"' | sed -E 's/^"//; s/"$//' || true)"
  if [ -z "${template_labels}" ]; then
    echo "Could not parse labels in: ${template}"
    missing=1
    continue
  fi

  type_count=0
  status_count=0
  while IFS= read -r label; do
    [ -n "${label}" ] || continue

    # Check that every template label exists in repository settings.
    if ! grep -Fqx "${label}" <<<"${existing_labels}"; then
      echo "Unknown label '${label}' in ${template}"
      missing=1
    fi

    # Count taxonomy dimensions enforced by the project policy.
    case "${label}" in
      type:*) type_count=$((type_count + 1)) ;;
      status:*) status_count=$((status_count + 1)) ;;
    esac
  done <<< "${template_labels}"

  # Enforce exactly one type and one status label per template.
  if [ "${type_count}" -ne 1 ]; then
    echo "Expected exactly one type:* label in ${template}, found ${type_count}"
    missing=1
  fi
  if [ "${status_count}" -ne 1 ]; then
    echo "Expected exactly one status:* label in ${template}, found ${status_count}"
    missing=1
  fi
done

# Fail the job if any template violates label policy.
if [ "${missing}" -ne 0 ]; then
  echo "Issue template label validation failed."
  exit 1
fi

echo "Issue template labels are valid."
