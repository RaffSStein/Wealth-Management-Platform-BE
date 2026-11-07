# platform-test

A test-support library. Services depend on it with `<scope>test</scope>` only.

## AbstractServiceSmokeTest
Starts the whole service (`@SpringBootTest`, random port) against real Postgres, Kafka and Redis containers
(Testcontainers), then checks that `GET <context-path>/actuator/health` answers `UP`.
It catches the failures unit tests miss: dependency updates (Spring Boot, Hibernate, Jackson, Kafka) that break bean
wiring, entity mapping, DDL or serializers when the app starts.

Each mature service has one subclass in its root test package, named `<Service>ApplicationSmokeTest`:

```java
class CustomerServiceApplicationSmokeTest extends AbstractServiceSmokeTest {
}
```

- Add service-specific overrides on the subclass with `@TestPropertySource(properties = ...)`
  (email-service, for example, turns off the mail health check because no SMTP server runs in the test).
- The containers start once per JVM. Kafka is wired through `kafka.broker.connectionUri` (the platform's key, see
  `KafkaConfiguration`), and Postgres and Redis through the standard `spring.datasource.*` / `spring.data.redis.*` keys.
- The images are pinned in `SmokeTestImages`.

## Running
The tests need Docker. Locally, without Docker, they are **skipped**, so the build still passes. When the `CI`
environment variable is `true` (GitHub Actions always sets it) they always run, so a CI runner without Docker fails
the build rather than silently skipping. GitHub's `ubuntu-latest` runners come with Docker.

```bash
./mvnw -pl business-modules/customer-service/customer-core -am test     # Windows: mvnw.cmd ...
```
