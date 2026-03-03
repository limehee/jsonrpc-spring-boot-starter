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
- Incoming response-side flow using classifier/parser/validator utilities

## Key Class

- `src/main/java/com/limehee/jsonrpc/sample/purejava/PureJavaDemoApplication.java`
- `src/main/java/com/limehee/jsonrpc/sample/purejava/ResponseSideUtilitiesExample.java`

The `main` method prints one output payload per scenario so you can follow request -> dispatch -> response flow.
