# CLAUDE.md

Context for Claude Code when working in this repository. Longer-form references: `README.md`, `CONTRIBUTING.md`,
`core/platform-core/README.md`, `.github/copilot-instructions.md`, and per-service `README.md` files.

## Project

Wealth Management Platform (WMP): an event-driven, Spring Boot microservices back end (work in progress).

- Java 21, Spring Boot 4.0.x, Maven multi-module (root `pom.xml` is the parent of everything).
- Kafka (CloudEvents envelope) for inter-service communication; PostgreSQL with one database per service.
- JSON logs to `./logs`, shipped to Splunk via a Universal Forwarder.

## Build & run

```bash
./mvnw -B -ntp clean install      # build all modules (Windows: mvnw.cmd ...)
./mvnw -ntp test                  # run tests
./mvnw -pl business-modules/customer-service/customer-core -am install   # one service + its deps
docker compose -f docker-compose.yml up -d                               # local infrastructure
```

- CI (`.github/workflows/maven.yml`) runs `clean install` then `test` on JDK 21 Temurin for pushes/PRs to `main`. Keep it green.
- The build regenerates OpenAPI code; a broken YAML spec breaks every dependent module.
- Local infra (`docker-compose.yml` + `.env`): Kafka + Kafka Connect + Kafbat UI, one `postgres-<service>` container per service
  (user/pass `test`/`test`), Splunk + forwarder, MinIO and fake-GCS (document storage), MailHog (SMTP `1025`, UI `8025`),
  WireMock external-provider stubs (`8094`, mappings in `integration/wiremock/mappings`, one path prefix per provider).
- Each service has its own HTTP port, context path `/<service-name>` and DB port. Check the service's `application.yaml`
  and `docker-compose.yml` before choosing a new port, so ports don't collide.

## Repository layout

```
core/platform-core/            shared library: security/JWT, errors, logging, Kafka base classes, audit, cache, retry, async
business-modules/<x>-service/  one microservice per domain
  <x>-api-data/                OpenAPI spec <x>-api-data.yaml -> generated REST interfaces + DTOs
  <x>-event-data/              OpenAPI spec <x>-event-data.yaml -> generated event payload classes
  <x>-core/                    Spring Boot app (raff.stein.<x>)
integration/                   external integrations
docs/domain/                   functional docs: process/ (markdown) + assets/ (diagrams .md + .png)
```

- Mature services follow the 3-submodule split above (customer, user, document, profiler, bank, email, proposal).
  order-service has only `order-core` so far. advisor, notification, portfolio, product and reporting are still flat
  skeletons. When fleshing one out, migrate it to the split structure.
- New modules must be registered in the root `pom.xml` `<modules>` list and in the root `README.md` module list.
- A service that consumes another service's events depends on that service's `*-event-data` module (e.g. email-core -> user-event-data).
  Never depend on another service's `*-core`.

## Architecture rules

### API-first (OpenAPI)
- Change the YAML spec first, rebuild, then implement. Controllers `implements` the generated `org.openapitools.api.*Api`
  interface (`interfaceOnly`, `skipDefaultInterface`, `requestMappingMode=api_interface`, `useTags`).
- Generated classes go to `org.openapitools.model` / `org.openapitools.api` in **every** module, so schema names must be
  unique platform-wide. Suffix REST models with `DTO` (`CustomerDTO`) and events with `Event` (`CustomerCreatedEvent`).
- Every operation has an `operationId` (camelCase), `summary`, `description` and `tags`. Paths are prefixed with the domain (`/customer/...`).
- Shared types (`ErrorResponse`, `ErrorCategory`) live in `platform-core/src/main/resources/api-data.yaml`. Reuse them.
  There is no shared pagination type yet: `Pageable` is defined only in `bank-api-data.yaml`. Move it to platform-core
  when another service needs paging.

### Layering and mapping (customer-service is the reference implementation)
- Flow: `DTO (api-data) -> controller mapper -> BO (model/bo) -> service -> entity mapper -> Entity (model/entity) -> repository`.
  Controllers never touch entities, and services never return DTOs.
- Package layout in `raff.stein.<x>`: `controller` (+ `controller/mapper/<area>`), `service/<area>`, `repository/<area>`,
  `model/bo/<area>`, `model/entity/<area>` (+ `enumeration`, `mapper`), `event/producer`, `event/consumer` (+ `mapper`),
  `client/<provider>` (Feign), `config`, `exception`.
- MapStruct mappers are interfaces named `<Source>To<Target>Mapper`. They expose a `MAPPER = Mappers.getMapper(...)` singleton,
  used as a `private static final` field. Each layer has a `@MapperConfig(uses = CommonMapperConfiguration.class)` config
  (e.g. `CustomerControllerCommonMapperConfig`, `CustomerEntityCommonMapperConfig`). Ignore generated IDs explicitly with `@Mapping(target = "id", ignore = true)`.
- BOs: Lombok `@Data @Builder`. Entities: `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`, `@Setter` only on mutable
  fields, `@Enumerated(EnumType.STRING)`, plural snake/lower table names, relationship collections marked
  `@ToString.Exclude @EqualsAndHashCode.Exclude @JsonIgnore`.
- Entities that need auditing/optimistic locking extend `BaseDateEntity<ID>` (created/modified by/at plus `@Version`).
- Repositories extend `JpaRepository`. Use `@EntityGraph` or fetch joins to avoid N+1 queries, and derived queries for simple lookups.
- Dependency injection is constructor-based via `@RequiredArgsConstructor` with `private final` fields. Use an explicit
  constructor only when `@Value` parameters are needed (see the event publishers).
- `@Transactional` goes on service methods only (`readOnly = true` for reads) and transactions stay short.
  Use `@OptimisticLockingRetry` only on idempotent writes.
- Multi-step domain logic uses strategy/dispatcher patterns: a handler interface plus a `@Component` dispatcher that builds
  a `Map<Key, Handler>` from the injected `List<Handler>` (see `OnboardingStepDispatcher`, `MifidWorkflowDispatcher`,
  `CustomerVisitorDispatcher`). To add a case, add a new handler implementation rather than an `if/switch`.

### Events (Kafka)
- Publishers are `@Component`s in `event/producer`, named `<EventName>EventPublisher`. They map to the generated event class,
  wrap it in `new EventData(payload)` and call `WMPBaseEventPublisher.publishCloudEvent(topic, eventData)`.
- Consumers are in `event/consumer`, named `<EventName>EventConsumer`. They extend `WMPBaseEventConsumer`, are gated with
  `@ConditionalOnProperty(name = "kafka.topics...enabled", havingValue = "true")`, use
  `@KafkaListener(topics = "${...name}", groupId = "${...groupId}", containerFactory = "kafkaListenerFactory")` and read the
  payload via `withEventPayload(...)` / `getEventPayload(...)`. Validate the payload defensively: on bad input, log a warning and return.
- Topics are declared per service in `<x>-core/src/main/resources/kafka-properties.yaml` under `kafka.topics.<owner-domain>`, using the naming pattern:
  - name: `${kafka.broker.environment}.${kafka.broker.tenant}.<domain>.<event-name>`
  - groupId: `${kafka.broker.environment}.${kafka.broker.tenant}.t.<domain>.<event-name>.sa.${kafka.broker.serviceAccount}`
  - Event names are kebab-case past tense (`customer-created`, `file-validated`, `user-created`).

### Configuration
- `application.yaml` (YAML, not `.properties`) imports `classpath:platform-shared-properties.yaml` and
  `classpath:kafka-properties.yaml` via `spring.config.import`. Override shared defaults (Hikari, Hibernate batching,
  cache, async executor, retry) per service rather than editing `platform-core`.
- Bind feature config with `@ConfigurationProperties` classes (e.g. `AmlProperties` with prefix `aml`). Platform-level keys
  live under `platform.*`, and service-level app settings under `wmp.*` (e.g. `wmp.onboarding.password-setup-link`).
- `@SpringBootApplication` classes add `@ComponentScan(basePackages = {"raff.stein.platformcore", "raff.stein.<x>"})`.
- There is no Flyway/Liquibase yet: the schema comes from `ddl-auto: update`, and seed data lives in `src/main/resources/sql/*.sql`.

### Errors, security, logging
- Throw `platform-core` exceptions (`RequestValidationException`, `GenericObjectNotFoundException`, `AccessDeniedException`,
  `VersionLockingException`, `NotImplementedException`, ...) or a service exception extending `GenericException`.
  `GlobalControllerExceptionHandler` maps them to `ErrorResponse` plus an HTTP status. Don't write per-controller try/catch blocks.
  New platform-wide codes go into `ErrorCode` with a `[%s]` message template.
- Security (JWT resource server, `WMPContext`, `SecurityContextFilter`) is centralized in `platform-core`. Don't reimplement it in services.
  The JWT keys and `local_token.txt` in `platform-core/src/main/resources/jwt` are **local-dev only**.
- Use Lombok `@Slf4j`. Log messages wrap values in brackets, e.g. `log.info("Processing X for customerId: [{}]", id)`.
  Use `warn` for expected/client issues and `error` for failures. Never log secrets, tokens or full PII payloads.
- Logback config is shared in `platform-core` (`logback-spring.xml`, JSON with traceId/spanId/correlation). Don't add per-service logback files.

## Coding style
- English only for code, comments, docs and commit messages.
- Use `final` for local variables, streams with `.toList()`, `Optional.orElseThrow(() -> ...)`, and Lombok over boilerplate.
- Keep comments short and explain *why*. `//TODO:` notes are an accepted way to flag known follow-ups.
- Don't add new dependencies unless justified. When adding one, declare its version as a property plus a
  `dependencyManagement` entry in the root POM, and reference it without a version in the module POM.

## Testing
- JUnit 5 + Spring Boot Test. Good examples are in `platform-core/src/test` (plain unit tests with `assertEquals`/`assertThrows`,
  method names like `shouldRetryAndSucceedWithinMaxAttempts`).
- Service test coverage is currently minimal (placeholder `@SpringBootTest` classes). When changing behavior, add tests covering
  the happy path and at least one edge case. Mock Kafka, external clients and the security context, and keep tests deterministic.

## Git workflow
- Conventional commits: `type(scope): imperative subject` with types `feat|fix|docs|refactor|test|chore`. The scope is the module
  (`customer-service`, `platform-core`, `email-service`) or area (`readme`, `gitignore`, `jwt`). Dependabot uses `chore(dependencies)` / `chore(actions)`.
- Keep commits small and atomic, and update OpenAPI specs, mappers, tests and READMEs in the same change as the behavior they describe.
- Commit and push only when asked. Commits are authored with the user's own git identity.
  **Never add `Co-Authored-By` trailers or any Claude/AI attribution to commits or PRs.**
- PR descriptions include rationale, risk and testing notes.

## Documentation
- When adding endpoints, events or config, update the service `README.md`. Its section order follows `document-service`/`email-service`:
  Overview, Module Structure, Setup, Data Model, Capabilities & APIs, Topics & Events, Key Processes, Miscellaneous, Work in Progress.
- Functional/process documentation goes in `docs/domain/process/<area>/`, with diagrams in `docs/domain/assets/<area>/`.
- Prefer cross-platform commands in docs, giving Windows equivalents next to Unix ones.
