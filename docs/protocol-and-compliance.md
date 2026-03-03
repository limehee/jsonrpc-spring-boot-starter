# Protocol and Compliance

This document maps current behavior to JSON-RPC 2.0 requirements.

## Normative References

- JSON-RPC 2.0: <https://www.jsonrpc.org/specification>
- RFC 8259 (JSON): <https://www.rfc-editor.org/rfc/rfc8259>

## Compliance Matrix

| Rule                    | Spec Requirement                     | Implementation Behavior                                                                                                                              |
|-------------------------|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `jsonrpc` field         | MUST be string `"2.0"`               | Validated; otherwise `-32600 Invalid Request`                                                                                                        |
| `method` field          | MUST be string                       | Validated non-null/non-blank; otherwise `-32600`                                                                                                     |
| `params` field          | MAY be array/object                  | If present and not array/object -> default `-32602 Invalid params` (configurable to `-32600 Invalid Request` via validator policy / Spring property) |
| `id` field type         | String/Number/Null (if present)      | Invalid `id` type -> `-32600`; error response id normalized to `null`                                                                                |
| Notification            | Request without `id`                 | Invoked with no response payload                                                                                                                     |
| Success response        | MUST contain `result` (no `error`)   | Enforced by `JsonRpcResponse` invariant                                                                                                              |
| Error response          | MUST contain `error` (no `result`)   | Enforced by `JsonRpcResponse` invariant                                                                                                              |
| Parse error             | Invalid JSON text                    | `-32700 Parse error`, `id: null`                                                                                                                     |
| Method not found        | Unknown method                       | `-32601 Method not found`                                                                                                                            |
| Internal error          | Unhandled runtime/checked exceptions | `-32603 Internal error`                                                                                                                              |
| Batch request           | Array of requests                    | Supported                                                                                                                                            |
| Empty batch             | Invalid request                      | Single error object with `-32600`                                                                                                                    |
| Notification-only batch | No response                          | HTTP adapter returns no body                                                                                                                         |

## Error Codes

| Code     | Meaning          |
|----------|------------------|
| `-32700` | Parse error      |
| `-32600` | Invalid Request  |
| `-32601` | Method not found |
| `-32602` | Invalid params   |
| `-32603` | Internal error   |

Implementation constants are in `JsonRpcErrorCode` and messages in `JsonRpcConstants`.

## Request Processing Semantics

1. Transport parses bytes into JSON.
2. Invalid JSON or whitespace-only body is treated as parse error (`-32700`).
3. Dispatcher validates request object shape.
4. Method handler is resolved.
5. Params are bound/invoked.
6. Result or error is composed into JSON-RPC response.

## Incoming Response Validation

`jsonrpc-core` also provides response-side protocol utilities:

- `JsonRpcEnvelopeClassifier`
- `JsonRpcResponseParser`
- `JsonRpcResponseValidator`
- `JsonRpcResponseValidationOptions`

These APIs are transport-agnostic and useful for bidirectional channels (for example WebSocket) where
request/response envelopes may arrive on the same connection.

### Default Validation Rules (RFC MUST)

By default, `JsonRpcResponseValidationOptions.defaults()` enforces:

- top-level response is an object
- `jsonrpc` equals `"2.0"`
- `id` member exists and is `string | number | null`
- exactly one of `result` or `error` is present
- when `error` is present:
  - `error` is an object
  - `error.code` is an integer
  - `error.message` is a string

RFC SHOULD or stricter interoperability policies are configurable via per-rule options.
This library does not expose predefined strict/lenient modes; policy is controlled per rule.

### Validation Options

`JsonRpcResponseValidationOptions` exposes per-rule switches:

- `requireJsonRpcVersion20` (default: `true`)
- `requireResponseIdMember` (default: `true`)
- `allowNullResponseId` (default: `true`)
- `allowStringResponseId` (default: `true`)
- `allowNumericResponseId` (default: `true`)
- `allowFractionalResponseId` (default: `true`)
- `requireExclusiveResultOrError` (default: `true`)
- `requireErrorObjectWhenPresent` (default: `true`)
- `requireIntegerErrorCode` (default: `true`)
- `requireStringErrorMessage` (default: `true`)
- `allowRequestFieldsInResponse` (default: `true`)

`allowRequestFieldsInResponse=true` is a compatibility default and is not an RFC MUST rule.

## `id` Handling Details

- Notification is defined by **absence** of `id` field (`idPresent == false`).
- `"id": null` is not treated as notification; it is still considered present.
- For malformed requests where `id` exists but type is not allowed (for example object/array), response uses `id: null`.

## Batch Behavior Details

- Batch input must be a JSON array.
- Each entry is evaluated independently.
- Non-object entries in batch produce `Invalid Request` responses.
- Responses include only non-notification calls.
- Result list order follows input traversal order.

## Reserved Method Namespace

Methods starting with `rpc.` are rejected at registration (`IllegalArgumentException`) to preserve reserved namespace semantics.

## HTTP Mapping Notes

Default WebMVC strategy returns:

- `200 OK` for single/batch, including protocol errors
- `204 No Content` for notification-only execution

This is transport policy, not protocol rule, and can be overridden via `JsonRpcHttpStatusStrategy`.

## Known Deliberate Policy Choices

- Oversized request body (`jsonrpc.max-request-bytes`) maps to protocol error `-32600` with message `Request payload too large`.
- Parse errors always use `id: null`.
- Generic exceptions are intentionally normalized to `-32603` to avoid leaking internals.
- `params` type violations (non-array/object) default to `-32602`; in Spring Boot this can be changed with `jsonrpc.validation.request.params-type-violation-code-policy=INVALID_REQUEST`.
