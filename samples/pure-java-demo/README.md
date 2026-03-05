# pure-java-demo

Runnable Pure Java JSON-RPC 2.0 sample without Spring.

## Run

From repository root:

```bash
./gradlew -p samples/pure-java-demo run
```

## What This Demo Covers

- Single request success flow
- Notification flow (no response body)
- Mixed batch flow (request + notification + error)
- Parse error flow
- Typed handler registration (`JsonRpcTypedMethodHandlerFactory`)
- Manual handler registration (`dispatcher.register`)
- Request-validator policy switch with `JsonRpcParamsTypeViolationCodePolicy`
- Request validation profile with `JsonRpcRequestValidationOptions` (`require-id-member`, `allow-fractional-id`,
  `reject-response-fields`)
- Response validation profile with `JsonRpcResponseValidationOptions` (`reject-request-fields`,
  `reject-duplicate-members`, `error-code.policy`)
- Incoming response-side flow using classifier/parser/validator utilities
- Interceptor lifecycle flow (`beforeValidate`, `beforeInvoke`, `afterInvoke`, `onError`)

## Key Class

- `src/main/java/com/limehee/jsonrpc/sample/purejava/PureJavaDemoApplication.java`
- `src/main/java/com/limehee/jsonrpc/sample/purejava/ResponseSideUtilitiesExample.java`
- `src/main/java/com/limehee/jsonrpc/sample/purejava/InterceptorFlowExample.java`
- `src/main/java/com/limehee/jsonrpc/sample/purejava/ValidationProfileExample.java`

The `main` method prints one output payload per scenario so you can follow request -> dispatch -> response flow.
