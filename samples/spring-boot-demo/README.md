# spring-boot-demo

Minimal JSON-RPC 2.0 demo application using `jsonrpc-spring-boot-starter`.

## Run

From repository root:

```bash
./gradlew -p samples/spring-boot-demo bootRun
```

The app starts on `http://localhost:8080` with endpoint `POST /jsonrpc`.

## Try Requests

Ping:

```bash
curl -s http://localhost:8080/jsonrpc \
  -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","method":"ping","id":1}'
```

Typed object params:

```bash
curl -s http://localhost:8080/jsonrpc \
  -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","method":"greet","params":{"name":"developer"},"id":2}'
```

Named params with `@JsonRpcParam`:

```bash
curl -s http://localhost:8080/jsonrpc \
  -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","method":"sum","params":{"left":2,"right":3},"id":3}'
```
