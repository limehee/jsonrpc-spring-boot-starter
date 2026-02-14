# Protocol and Compliance

This document maps current behavior to JSON-RPC 2.0 requirements.

## Normative References

- JSON-RPC 2.0: <https://www.jsonrpc.org/specification>
- RFC 8259 (JSON): <https://www.rfc-editor.org/rfc/rfc8259>

## Compliance Matrix

| Rule | Spec Requirement | Implementation Behavior |
|---|---|---|
| `jsonrpc` field | MUST be string `"2.0"` | Validated; otherwise `-32600 Invalid Request` |
| `method` field | MUST be string | Validated non-null/non-blank; otherwise `-32600` |
| `params` field | MAY be array/object | If present and not array/object -> `-32602 Invalid params` |
| `id` field type | String/Number/Null (if present) | Invalid `id` type -> `-32600`; error response id normalized to `null` |
| Notification | Request without `id` | Invoked with no response payload |
| Success response | MUST contain `result` (no `error`) | Enforced by `JsonRpcResponse` invariant |
| Error response | MUST contain `error` (no `result`) | Enforced by `JsonRpcResponse` invariant |
| Parse error | Invalid JSON text | `-32700 Parse error`, `id: null` |
| Method not found | Unknown method | `-32601 Method not found` |
| Internal error | Unhandled runtime/checked exceptions | `-32603 Internal error` |
| Batch request | Array of requests | Supported |
| Empty batch | Invalid request | Single error object with `-32600` |
| Notification-only batch | No response | HTTP adapter returns no body |

## Error Codes

| Code | Meaning |
|---|---|
| `-32700` | Parse error |
| `-32600` | Invalid Request |
| `-32601` | Method not found |
| `-32602` | Invalid params |
| `-32603` | Internal error |

Implementation constants are in `JsonRpcErrorCode` and messages in `JsonRpcConstants`.

## Request Processing Semantics

1. Transport parses bytes into JSON.
2. Invalid JSON or whitespace-only body is treated as parse error (`-32700`).
3. Dispatcher validates request object shape.
4. Method handler is resolved.
5. Params are bound/invoked.
6. Result or error is composed into JSON-RPC response.

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
