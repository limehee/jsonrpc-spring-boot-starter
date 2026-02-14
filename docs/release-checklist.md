# Release Checklist

Use this checklist before creating a release tag (`vX.Y.Z`).

## 1) Version and Changelog

- Update `gradle.properties` `version` to the release version (remove `-SNAPSHOT`).
- Ensure `CHANGELOG.md` reflects all user-visible changes.
- Confirm README examples and configuration docs match the current code.

## 2) Local Verification

- Run full verification:
  - `./gradlew clean check`
  - `./gradlew test integrationTest e2eTest`
- Run API compatibility check against latest released version:
  - `./gradlew apiCompat -PapiBaselineVersion=<latest released version>`
- Run benchmark smoke:
  - `./gradlew :jsonrpc-core:jmhQuick`
- Run consumer smoke verification:
  - `./scripts/verify-consumer-smoke.sh`

## 3) Publication Preconditions

- Confirm OSSRH credentials are available:
  - `OSSRH_USERNAME`, `OSSRH_PASSWORD`
- Confirm signing credentials are available:
  - `SIGNING_KEY`, `SIGNING_PASSWORD`
- Validate generated artifacts:
  - jars, sources jar, javadoc jar, signatures, pom metadata

## 4) Tag and Publish

- Commit release changes.
- Create annotated git tag:
  - `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
- Push branch and tag:
  - `git push`
  - `git push origin vX.Y.Z`
- Verify GitHub Actions `Publish` workflow completed successfully.

## 5) Post-Release

- Bump `gradle.properties` version to next snapshot (for example `X.Y.(Z+1)-SNAPSHOT`).
- Add release notes entry (GitHub release and changelog link).
- Smoke test the sample project with the published version.
