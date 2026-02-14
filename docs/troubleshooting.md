# Troubleshooting

## Startup Failure: Invalid `jsonrpc.*` Property

Symptom:

- Application fails at startup with `IllegalArgumentException` related to `jsonrpc.*`.

Cause:

- Fail-fast configuration validation rejects invalid values.

Checks:

- `jsonrpc.path` starts with `/` and has no whitespace
- `jsonrpc.max-batch-size > 0`
- `jsonrpc.max-request-bytes > 0`
- allowlist/denylist entries are not blank
- `jsonrpc.notification-executor-bean-name` is not null

## Method Returns `-32601 Method not found`

Possible causes:

- Method not registered (annotation scan disabled or bean not detected)
- Method blocked by allowlist/denylist policy
- Method name mismatch between request and registration

Checks:

- `jsonrpc.scan-annotated-methods` value
- `@JsonRpcMethod` name or registration name
- `jsonrpc.method-allowlist` / `jsonrpc.method-denylist`

## Params Return `-32602 Invalid params`

Possible causes:

- `params` shape does not match method signature
- Missing named argument in object mode
- Positional array size mismatch
- Jackson conversion failure for target type

Checks:

- For multi-arg object params, confirm `@JsonRpcParam` names
- For reflection names, ensure build uses `-parameters`
- Validate incoming JSON types

## Notification Did Not Return Body

This is expected JSON-RPC behavior when `id` is omitted.

If response is required, include `id` in request.

## Executor Configuration Not Applied

Symptom:

- Notifications still appear synchronous.

Checks:

- `jsonrpc.notification-executor-enabled=true`
- named executor bean exists if `notification-executor-bean-name` is set
- if multiple executors exist, choose one explicitly

## Parse Errors for Empty or Whitespace Body

Behavior:

- Empty/whitespace-only payload is treated as parse error (`-32700`).

Action:

- Ensure clients send valid JSON request object or batch array.

## Oversized Payload Error

Behavior:

- Request larger than `jsonrpc.max-request-bytes` returns protocol error payload.

Action:

- Increase limit if use case requires larger requests.
- Consider splitting large payload into smaller calls.
