# Testing and Quality

The project uses layered tests across modules to protect protocol correctness and integration behavior.

## Test Stages

Each module can run:

- `test`: unit tests
- `integrationTest`: module integration tests
- `e2eTest`: end-to-end tests

Commands:

```bash
./gradlew test
./gradlew integrationTest
./gradlew e2eTest
./gradlew check
```

`check` depends on all three stages.

## Coverage Focus Areas

### Core (`jsonrpc-core`)

- Parser/validator rules (`jsonrpc`, `method`, `params`, `id`)
- Dispatcher branches:
  - success
  - invalid request
  - method missing
  - invalid params
  - internal exceptions
  - notification no-response
  - batch (mixed/single/empty)
- Interceptor callbacks and error resilience
- Typed binder/writer behavior for records/classes/collections
- Pure Java integration and e2e usage

### WebMVC (`jsonrpc-spring-webmvc`)

- Parse error handling
- Request byte limit handling
- Notification HTTP behavior
- Custom/default status strategy behavior
- Response serialization stability

### Auto-configuration (`jsonrpc-spring-boot-autoconfigure`)

- Bean wiring and override points
- Property-driven behavior
- Access control precedence
- Notification executor selection
- Annotation/manual/typed registration styles
- Integration/e2e application-level method execution

### Sample app (`samples/spring-boot-demo`)

- Bean/method registration assertions
- End-to-end request/response payload assertions
- allowlist/denylist behavior
- path/request-limit customization scenarios

## API Compatibility

Binary compatibility checks are provided via JApiCmp:

```bash
./gradlew apiCompat -PapiBaselineVersion=<released-version>
```

## CI

GitHub Actions runs matrix tests and compatibility checks (when release tag baseline exists):

- `.github/workflows/ci.yml`
- `.github/workflows/publish.yml`
- `.github/workflows/consumer-smoke.yml` (publishes to `mavenLocal` and verifies Maven/Gradle consumer projects)

## Quality Expectations for Contributions

- Add tests for success/failure/exception branches
- Keep protocol behavior aligned with JSON-RPC 2.0
- Update docs and changelog for user-visible behavior changes
