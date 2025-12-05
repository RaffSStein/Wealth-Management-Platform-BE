# platform-core module

`platform-core` is the shared core library for all Wealth Management Platform (WMP) microservices. It centralizes cross-cutting concerns (error handling, logging, tracing, messaging abstractions, security configuration, shared properties) so that business services can stay focused on domain logic.

## Architecture and design goals

- Provide a single, consistent implementation for platform-level concerns across all `*-service/*-core` modules.
- Minimize duplication of boilerplate (exception mapping, logging filters, Kafka wiring, shared configuration).
- Be opinionated but overridable: expose sensible defaults while allowing service-level customization.
- Support production-grade observability, resiliency, and traceability for a distributed banking environment.
- Keep `platform-core` free of business/domain logic; only platform and infrastructure concerns belong here.

## Usage in business microservices

### Adding the dependency

Each `*-service/*-core` module should depend on `platform-core` through Maven. The exact coordinates and version are managed by the root `pom.xml` and the service module POMs.

### Typical usage patterns

- REST controllers:
    - Throw shared exceptions such as `RequestValidationException`, `GenericObjectNotFoundException`, or `VersionLockingException`.
    - Rely on `GlobalExceptionHandler` for consistent error mapping and logging.
- Service layer:
    - Use `GenericException` or specialized subclasses for cross-cutting conditions.
    - Log with the shared logging configuration, ensuring `traceId` is present on all relevant logs.
- Messaging components:
    - Use `EventPublisher` or `WMPBaseEventPublisher` to send events.
    - Implement `EventConsumer` or extend `WMPBaseEventConsumer` to process events with consistent tracing and security context.


## Core features overview

The `platform-core` module groups several cross-cutting features that can be reused by all business services:

- [Shared exception and error handling](#shared-exception-and-error-handling)
- [Logging and HTTP request/response tracing](#logging-and-http-requestresponse-tracing)
- [Distributed tracing and observability](#distributed-tracing-and-observability)
- [Messaging abstractions (Kafka)](#messaging-abstractions-kafka)
- [Shared configuration and properties](#shared-configuration-and-properties)
- [JWT and security support](#jwt-and-security-support)
- [Cross-cutting patterns for business microservices](#cross-cutting-patterns-for-business-microservices)
- [Optimistic locking and retry](#optimistic-locking-and-retry)
- [Shared async TaskExecutor and `@Async`](#shared-async-taskexecutor-and-async)

Business microservices depend on `platform-core` as a Maven module to automatically inherit these behaviors.

## Shared exception and error handling

### Error model and categories

`platform-core` standardizes how errors are represented and exposed through REST APIs:

- `ErrorCode`
  - Centralized enumeration of platform error codes.
  - Each code is mapped to a specific `ErrorCategory` that is visible in the OpenAPI model.
  - Categories typically cover: validation errors, authentication and authorization failures, not-found errors, conflicts, and generic server errors.
- OpenAPI alignment
  - The internal error model is aligned with shared OpenAPI types (for example `org.openapitools.model.ErrorResponse` and `ErrorCategory`).
  - This ensures that all services expose a consistent error schema on their REST endpoints.

### Exception hierarchy

The module defines a shared exception hierarchy to express cross-cutting error semantics:

- `GenericException`
  - Base runtime exception for platform errors.
  - Carries an `ErrorCode`, a human-readable message, and optionally a cause and extra metadata.
- Specialized exceptions (examples):
  - `RequestValidationException` – input validation or business rule violations (HTTP 400).
  - `AuthenticationException`, `JwtTokenException`, `WmpContextException` – authentication and context-related issues (HTTP 401).
  - `AccessDeniedException` – authorization failures (HTTP 403).
  - `GenericObjectNotFoundException` – missing domain objects (HTTP 404).
  - `VersionLockingException` – optimistic locking / concurrent modification conflicts (HTTP 409).
  - `NotImplementedException` – functionality defined but not yet implemented.

Services should prefer these shared exception types over ad-hoc ones whenever the semantics are platform-wide.

### Global exception handling

`platform-core` provides controller advice that centralizes error-to-HTTP mapping:

- `GlobalExceptionHandler` (and complementary handlers where present):
  - Declared as `@ControllerAdvice` so it is auto-discovered by Spring Boot.
  - Maps known platform exceptions to appropriate `HttpStatus` codes and `ErrorResponse` payloads.
  - Attaches the current distributed trace identifier (via Micrometer `Tracer`) to the `ErrorResponse` for correlation.
  - Applies a consistent logging strategy:
    - `WARN` for expected client-side errors (validation, not-found, etc.).
    - `ERROR` for unexpected server-side failures.
- Catch-all handling
  - Any unhandled `Exception` is translated into a generic 500 error with a safe, non-sensitive message.

By simply depending on `platform-core`, a microservice gets this consistent behavior without additional configuration. Local `@ControllerAdvice` classes can still be used for service-specific rules if needed.

## Logging and HTTP request/response tracing

### Shared logging configuration

Logging is standardized through a shared Logback configuration:

- `logback-spring.xml` (in `platform-core` resources) defines:
  - Log pattern and structure (timestamp, level, service identifier, `traceId`/`spanId`, and other contextual fields).
  - Appenders and encoders (for example JSON layout for centralized log aggregation, or text layout for local development).
  - Profile-specific tuning, where applicable (for example, more verbose logging in `local` profile).
- Microservices inherit this configuration automatically if they do not override `logback-spring.xml`.

This ensures logs from different services can be correlated and processed uniformly.

### HTTP request/response logging filter

`platform-core` exposes a servlet filter that consistently logs HTTP traffic:

- `RequestResponseLoggingFilter` (or equivalent):
  - Logs HTTP method, path, response status, and latency.
  - Includes correlation identifiers (for example, `traceId`, `spanId`, user or tenant identifiers when available).
  - Optionally logs headers and body excerpts within safe limits to avoid performance regressions and data leaks.
- Integration:
  - Registered as a Spring bean so it is automatically part of the filter chain for all REST endpoints.

This filter provides a unified view of inbound and outbound HTTP interactions across all microservices.

## Distributed tracing and observability

`platform-core` integrates with Micrometer Tracing to enable end-to-end tracing:

- `io.micrometer.tracing.Tracer` is injected where needed (for example, in exception handlers and messaging components).
- The current `traceId` is attached to error responses and is expected to be present in log entries.
- HTTP and Kafka flows share the same tracing model, enabling cross-boundary correlation.

Microservices are expected to export traces to the configured tracing backend (OpenTelemetry, Zipkin, etc.), as defined at the platform level.

## Messaging abstractions (Kafka)

### Shared Kafka properties

Kafka connectivity and common producer/consumer settings are centralized in resources:

- `kafka-shared-properties.yaml` defines properties such as:
  - `kafka.broker.connectionUri` and other broker connection parameters.
  - Service account and security settings for secure connections.
  - Shared metadata like `environment`, `tenant`, `domain`, and source identifiers.
- `platform-shared-properties.yaml` imports the Kafka properties using `spring.config.import` so that they are visible to Spring Boot auto-configuration.

Individual services can override or extend these properties using their own `application-*.yaml` files while still benefitting from the shared defaults.

### Publisher abstractions

`platform-core` provides base types for publishing events to Kafka:

- `EventPublisher`
  - Core interface for sending events.
  - Encapsulates topic, key, and payload handling.
- `WMPBaseEventPublisher`
  - Base implementation that:
    - Wraps domain payloads in a shared envelope (for example `EventData`).
    - Applies standard headers (trace identifiers, tenant, source service, etc.).
    - Logs outgoing events in a structured and consistent way.

Microservices should inject or extend these publishers to ensure consistent event shapes and metadata.

### Consumer abstractions

For Kafka consumers, `platform-core` offers a set of reusable components:

- `EventConsumer`
  - Interface defining the contract for event handlers.
- `WMPBaseEventConsumer`
  - Base class that:
    - Deserializes the shared `EventData` envelope.
    - Extracts and propagates tracing and security context from event headers.
    - Centralizes basic error handling and logging.
- `raff.stein.platformcore.messaging.consumer.config`
  - Provides Spring `@Configuration` for Kafka consumers (for example, consumer factory and listener container factory beans).
  - Integrates with security context initialization strategies so that event processing can reuse the same authentication and authorization model used for HTTP.

Business microservices implement their own concrete consumers on top of these abstractions.

## Shared configuration and properties

### `platform-shared-properties.yaml`

This resource centralizes configuration that is common across services:

- JWT and security:
  - `security.jwt.publicKeyPath` – path to the public key used to verify tokens.
  - `security.jwt.header` and `security.jwt.prefix` – conventions for the HTTP authorization header.
- Spring configuration:
  - Properties that fine-tune default Spring Boot behavior (for example, disabling template checks where not used).
- Kafka configuration import:
  - `spring.config.import` to pull in `kafka-shared-properties.yaml`.

Services are expected to import or extend these shared properties, overriding them only where necessary for local concerns.

### JWT and security support

The `jwt/` resource subtree documents and supports the platform JWT model:

- Keys and tokens:
  - `public_key.pem` and `private_key.pem` (or their equivalents) for signing and verification.
  - Sample tokens (for example a `local_token.txt`) to simplify local testing.
- Documentation:
  - `jwt_configuration.md` describes JWT structure, required claims, and validation rules.

Services should follow these conventions when building authentication filters and WMP context handling, so that JWT behavior is consistent across the platform.

## Cross-cutting patterns for business microservices

### Validation and request handling

- Use `RequestValidationException` for application-level validation errors that go beyond standard Bean Validation.
- Use Bean Validation annotations (`@Valid`, `@NotNull`, etc.) in combination with the shared exception handling to get uniform error responses.
- Prefer platform exceptions and error codes wherever the semantics are shared across multiple services.

### Pagination and shared DTOs

- `api-data.yaml` in `platform-core` defines shared OpenAPI types such as `ErrorResponse` and, where applicable, pagination-related models.
- Generated DTOs (for example in `org.openapitools.model`) should be reused by services when exposing or consuming shared contracts.

### Logging, security, and context propagation

- Always rely on the tracing and logging conventions defined by `platform-core` (trace identifiers, log structure).
- For Kafka flows, use the shared consumer configuration and security context initialization utilities so that event processing uses the same identity information as HTTP calls.
- Use the shared exception types for security-related problems so they are surfaced consistently.


## Optimistic locking and retry


`platform-core` exposes a common pattern for optimistic locking handling and retry. Business modules should adopt it consistently to achieve predictable behavior under concurrent updates.

#### 1. Use `BaseDateEntity` and `@Version` on JPA entities

- Extend the shared base entity provided by `platform-core` (for example `raff.stein.platformcore.model.BaseDateEntity` or equivalent) for all aggregate roots that require optimistic locking and audit information.
- Ensure that the base class (or the concrete entity) declares a `@Version` field, for example:
  - `@Version private Long version;`
- Apply this pattern across write-heavy entities (orders, proposals, portfolios, customer aggregates) where concurrent updates are expected.
- Do not mix optimistic locking with long-running transactions; keep transactions as short as possible.

#### 2. Configure retry behavior via properties

`platform-core` provides configuration properties (for example `OptimisticLockingRetryProperties`) that define the retry policy for optimistic locking conflicts. These properties are bound from `application.properties` or `application.yml` using a shared prefix, for example:

```properties
optimistic-locking.retry.max-attempts=3
optimistic-locking.retry.backoff-delay-ms=50
optimistic-locking.retry.backoff-multiplier=2.0
```

or in YAML:

```yaml
optimistic-locking:
  retry:
    max-attempts: 3
    backoff-delay-ms: 50
    backoff-multiplier: 2.0
```

Guidelines for business modules:

- Configure these properties **per microservice** based on its workload and SLOs.
- Keep `max-attempts` small (typically 2–5) to avoid cascading retries under heavy contention.
- Use conservative delays and multipliers to reduce the risk of retry storms.
- Treat these settings as operational knobs that can be tuned per environment (local, dev, prod) without code changes.

#### 3. Annotate idempotent write operations with `@OptimisticLockingRetry`

The `raff.stein.platformcore.optimisticlocking.annotation.OptimisticLockingRetry` annotation wraps a method with an optimistic-lock-aware retry policy built on top of Spring Retry.

Typical usage in a business service:

```java
@Service
public class ProposalService {

    @OptimisticLockingRetry
    @Transactional
    public Proposal updateProposal(ProposalUpdateRequest request) {
        // load aggregate with @Version
        // apply changes
        // persist via repository
        // any OptimisticLockingFailureException will trigger a retry
    }
}
```

Key points:

- Apply `@OptimisticLockingRetry` only to **idempotent** or **semantically safe-to-retry** methods.
- The annotation is intended mainly for service-layer methods that encapsulate a single logical update of an aggregate.
- On an `OptimisticLockingFailureException` (or `ObjectOptimisticLockingFailureException`), the method invocation is transparently retried according to the configured properties.
- After the last attempt, failures should be mapped to `VersionLockingException` and then to an HTTP 409 Conflict by the shared exception handler.

#### 4. Combine retry with clear API semantics

For REST APIs exposed by business modules:

- Make it explicit in API documentation (OpenAPI) that certain endpoints are subject to optimistic locking and may respond with HTTP 409 in case of conflicts.
- Use shared error types (for example, `ErrorResponse` with an appropriate `ErrorCode`/`ErrorCategory`) to describe optimistic locking failures.

This combination of `BaseDateEntity` + `@Version` + `@OptimisticLockingRetry` + shared properties ensures that all services handle concurrent updates and retries in a uniform and observable way.


## Shared async TaskExecutor and `@Async`

To support concurrent processing without blocking HTTP request threads, `platform-core` exposes a shared `TaskExecutor` and patterns for `@Async` usage.

### Provided executor bean

- `platformTaskExecutor` (type `ThreadPoolTaskExecutor`)
  - Defined in `raff.stein.platformcore.bean.PlatformCoreBeans`.
  - Enabled for Spring `@Async` via `@EnableAsync` on the same configuration.
  - Configured via `platform.async.task-executor.*` properties (see below).
  - Uses a `TracingTaskDecorator` so that Micrometer tracing / observation context is propagated to async threads.

Recommended usage in business microservices:

- Annotate asynchronous methods with `@Async("platformTaskExecutor")` to run them on the shared executor.
- Keep async methods short-lived and non-blocking where possible; prefer Kafka and event-driven flows for long-running work.

### Configuration properties

`platform-core` binds executor settings from configuration using `AsyncTaskExecutorProperties` (`platform.async.task-executor` prefix).

Example YAML configuration:

```yaml
platform:
  async:
    task-executor:
      core-pool-size: 10
      max-pool-size: 50
      queue-capacity: 1000
      keep-alive-seconds: 60
      thread-name-prefix: "platform-async-"
      rejection-policy: "CALLER_RUNS" # allowed values: CALLER_RUNS, ABORT, DISCARD, DISCARD_OLDEST
      wait-for-tasks-to-complete-on-shutdown: true
      await-termination-seconds: 30
```

Key behaviors:

- **Thread pool sizing**
  - `core-pool-size` / `max-pool-size`: control concurrency; tune per microservice based on workload and resources.
  - `queue-capacity`: bounds the number of queued tasks; too large values may hide overload, too small may cause early rejections.
- **Thread naming**
  - `thread-name-prefix` defaults to `platform-async-` so executor threads appear as `platform-async-1`, `platform-async-2`, ... in logs and debuggers.
- **Rejection policy** (`rejection-policy`)
  - `CALLER_RUNS` (default): task is executed in the calling thread when the pool and queue are saturated; this naturally back-pressures callers.
  - `ABORT`: throws a `RejectedExecutionException` when saturated.
  - `DISCARD`: silently discards the new task.
  - `DISCARD_OLDEST`: discards the oldest queued task in favor of the new one.
- **Shutdown behavior**
  - `wait-for-tasks-to-complete-on-shutdown`: whether to wait for running tasks during context shutdown.
  - `await-termination-seconds`: maximum seconds to wait for active tasks to finish.

### Tracing and observability

- The shared executor uses `TracingTaskDecorator`, which relies on Micrometer Observation to propagate the current observation/tracing context (`traceId`, span) into async threads.
- Any code executed via `@Async("platformTaskExecutor")` will see the same tracing context as the calling thread, so logs and metrics remain correlated across async boundaries.

### Service-level guidance

- Configure executor properties **per microservice** using that service's `application-*.yaml`, overriding the shared defaults when needed.
- Use the shared executor for short-lived background tasks (for example, fire-and-forget notifications, light recalculations).
- Prefer Kafka and the messaging abstractions in `platform-core` for durable, long-running, or externally observable workflows.

### Usage example in a business module

In a `*-service/*-core` module you can use the shared executor as follows:

1. Configure (optionally) service-specific executor settings in `application.yml`:

```yaml
platform:
  async:
    task-executor:
      core-pool-size: 5
      max-pool-size: 20
      queue-capacity: 200
      thread-name-prefix: "customer-async-"
```

2. Annotate a service method to run asynchronously on the shared executor:

```java
@Service
public class CustomerNotificationService {

    @Async("platformTaskExecutor")
    public void sendWelcomeEmail(String customerId) {
        // perform non-blocking, short-lived work here
        // e.g. prepare email payload and delegate to email-service
    }
}
```

- The `@Async("platformTaskExecutor")` annotation ensures that the method is executed
  on the shared, instrumented thread pool defined in `platform-core`.
- No additional `@EnableAsync` configuration is required in the business module, as
  it is already provided by `PlatformCoreBeans`.

## Database throughput and shared tuning

Database throughput is a key factor in a high-throughput banking platform. `platform-core` provides shared patterns and, where appropriate, shared configuration for Postgres data access.
The goal is to offer **safe, overridable defaults** that every microservice can adopt, while leaving final tuning decisions to each `*-service/*-core` module.

### Shared vs service-specific responsibilities

- `platform-core`:
  - Provides shared configuration files (`platform-shared-properties.yaml`, `kafka-shared-properties.yaml`, `db-shared-properties.yaml`).
  - Defines **baseline** HikariCP and Hibernate settings for Postgres.
  - Documents recommended value ranges and how to override them.
- Business microservices (`*-service/*-core`):
  - Own final values for pool sizes, timeouts, batching, and indexes.
  - Must validate their DB behavior via service-level load tests.
  - Are responsible for domain-specific schema design and indexing.

### Shared DB configuration: `db-shared-properties.yaml`

`platform-core` exposes a shared DB configuration file under `src/main/resources/db-shared-properties.yaml`.
It is imported by `platform-shared-properties.yaml` so that all services automatically inherit its defaults.

The shared DB properties provide conservative defaults for HikariCP and Hibernate:

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 2000   # ms, time to wait for a connection from the pool
      idle-timeout: 600000       # ms, 10 minutes
      max-lifetime: 1800000      # ms, 30 minutes, should be < DB or LB connection max lifetime

  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50         # enable conservative batching for services that use bulk writes
        order_inserts: true
        order_updates: true
```

Guidance:

- These values are **starting points**, not hard rules. Each microservice should override them where needed.
- All properties use standard Spring Boot / Hibernate names so they can be overridden directly in service `application-*.yaml`.
- Batch settings should be enabled only for services that actually perform bulk writes and have been tested under load.

### Connection pooling (HikariCP)

Connection pooling is one of the most impactful levers for DB throughput and stability.

**Baseline (shared):**

- `minimum-idle`: 5
- `maximum-pool-size`: 20
- `connection-timeout`: 2000 ms
- `idle-timeout`: 600000 ms (10 minutes)
- `max-lifetime`: 1800000 ms (30 minutes)

These defaults are meant for **small to medium** services in non-extreme load scenarios.

**Service-level tuning:**

Each `*-service/*-core` module should review and, if necessary, override HikariCP settings in its own `application-*.yaml`, for example:

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 40
      connection-timeout: 3000
```

Recommendations:

- Size `maximum-pool-size` based on:
  - service concurrency (HTTP thread pool, async executors);
  - how DB-bound the service is;
  - Postgres `max_connections` and the number of microservice instances.
- Ensure that the **sum** of all pools across services and instances + admin connections remains safely below `max_connections`.
- Keep `connection-timeout` in the range 2000–5000 ms and ensure it is **shorter** than the HTTP timeout for the same request.
- Set `max-lifetime` slightly below any DB-level or load-balancer connection lifetime to avoid using half-closed connections.

### Transaction boundaries and timeouts

Transaction design has a direct impact on contention and throughput.

Shared principles (to be applied in all services):

- Place `@Transactional` on **service-layer** methods, not on controllers.
- Keep transactions **short-lived**: avoid long-running business logic, remote HTTP calls, or blocking I/O inside the same transaction.
- Combine transactions with optimistic locking where appropriate (see [Optimistic locking and retry](#optimistic-locking-and-retry)).

Optional baseline properties (services may adopt them explicitly):

```yaml
spring:
  transaction:
    default-timeout: 10  # seconds, avoid indefinitely long transactions
```

Timeout alignment guidelines:

- HTTP server timeouts (for example, Tomcat/Netty) should be **greater** than DB transaction and query timeouts.
- Hikari `connection-timeout` should be **smaller** than the HTTP timeout, so that a lack of DB connections surfaces quickly.

### Query design, pagination, and result size control

Every microservice should design queries with throughput and resource usage in mind:

- Always paginate list endpoints.
  - Expose `page`/`size` (and optionally `sort`) parameters.
  - Enforce a reasonable default page size (for example, 20–50) and a maximum page size (for example, 100) per service.
- Avoid N+1 query patterns.
  - Use `@EntityGraph`, fetch joins, or DTO projections to load related data efficiently.
- Prefer projections / DTO queries for large result sets instead of loading full entities and their graph.
- Always specify an `ORDER BY` when using pagination to get deterministic results.

If a service uses shared pagination DTOs from `platform-core` or its own `api-data` module, make sure the REST layer and repositories are aligned on page/size semantics.

### Indexing and schema design

Indexing and schema design are **service-specific** responsibilities, but platform-wide guidelines apply:

- Ensure that primary keys and foreign keys are indexed.
- Add indexes on columns that appear frequently in `WHERE`, `JOIN`, and critical `ORDER BY` clauses.
- Use unique constraints for natural keys (for example, IBAN, external customer ID) instead of ad-hoc uniqueness checks in code.
- Manage indexes through your migration tool (for example, Liquibase/Flyway) in each microservice.

Microservices should regularly review slow queries (for example, via Postgres logs or APM) and adjust indexes accordingly.

### Batching and bulk operations

For services that perform batch inserts or updates (imports, nightly jobs, mass recalculations), Hibernate batching can significantly improve throughput:

- Shared defaults in `db-shared-properties.yaml` enable conservative batching:
  - `hibernate.jdbc.batch_size: 50`
  - `hibernate.order_inserts: true`
  - `hibernate.order_updates: true`
- Services that **do not** rely on bulk operations can keep these defaults as-is or partially override them.

Service-level guidance:

- Use `saveAll` or dedicated batch operations rather than single-row `save` in loops.
- Validate batching configuration under load to ensure it does not introduce unexpected lock contention.
- Consider separate profiles (for example, `batch` vs `default`) if a service has very different runtime modes.

### Timeouts and slow query protection

Time-based limits protect the platform from runaway queries and resource exhaustion.

Levers available to services:

- Hikari connection timeout (see above).
- JPA / Hibernate query timeouts, for example via properties:

  ```yaml
  spring:
    jpa:
      properties:
        javax:
          persistence:
            query:
              timeout: 4000  # ms, adjust per service
  ```

- HTTP timeouts at the server and API gateway level.

Guidelines:

- Set DB query timeouts to a value **lower than** the HTTP timeout for the same request.
- Monitor and review slow queries regularly; do not rely solely on timeouts to hide performance problems.

### Locking and concurrency

`platform-core` already provides shared support for optimistic locking and retry. DB tuning should be aligned with those patterns:

- Prefer optimistic locking with `@Version` and `@OptimisticLockingRetry` for concurrent updates.
- Avoid long-held locks by keeping transactions short and avoiding full-table scans on hot tables.
- Use pessimistic locks (`SELECT ... FOR UPDATE`) only in exceptional cases where business invariants cannot be protected otherwise, and document those cases per service.

### Environment-specific tuning

DB tuning is environment dependent. Typical patterns:

- **local/dev**:
  - Small pools (for example, `maximum-pool-size` 5–10).
  - Short timeouts, to surface misconfigurations early and avoid exhausting local resources.
- **test/stage**:
  - Similar properties to production but scaled down.
  - Used to validate behavior under realistic (but not full production) load.
- **prod**:
  - Pool sizes and timeouts tuned based on real traffic, hardware capacity, and SLOs.
  - Properties typically provided via environment variables or externalized configuration.

Examples of profile-specific overrides in a business microservice:

```yaml
# application-local.yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1

# application-prod.yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 40
      minimum-idle: 10
```

### How to adopt these patterns in a new or existing microservice

When creating or tuning a `*-service/*-core` module:

1. Ensure the service imports `platform-shared-properties.yaml` (and thus `db-shared-properties.yaml`) so the shared defaults are applied.
2. Review HikariCP settings in your service `application-*.yaml` and adjust `maximum-pool-size`, `minimum-idle`, and timeouts according to your workload.
3. Make sure all list endpoints use pagination and that repositories are designed accordingly.
4. Review key read and write queries, add or adjust indexes via your migration scripts.
5. Enable and tune batching only if your service performs bulk operations.
6. Align DB timeouts with HTTP and async processing patterns.

By following these shared patterns and properties, business microservices can achieve consistent, predictable database
behavior while retaining full control over service-specific tuning.

## Work in progress: caching support

Caching is a powerful lever for reducing load on downstream systems and improving response times. In a distributed banking context, it must be applied carefully.

Planned directions:

- Provide shared configuration for Spring Cache abstraction, including:
  - Recommended cache names for common use cases (for example, reference data such as products or risk profiles).
  - Default time-to-live (TTL) values and eviction policies.
- Support for pluggable cache providers:
  - Local in-memory cache (for example, Caffeine) for development and simple scenarios.
  - Distributed cache (for example, Redis) for cross-instance caching in production.
- Observability and safety:
  - Provide metrics for cache hit/miss and size.
  - Document guidelines to avoid caching sensitive or highly volatile data (for example, balances, orders, or personal information).


## Work in progress: HTTP server and Tomcat tuning


`platform-core` will document and, where appropriate, provide shared defaults for HTTP server tuning to support high concurrency and throughput.

Planned directions:

- Standardize recommendations for embedded server configuration (for example, Tomcat):
  - Maximum number of request processing threads.
  - Connection limits, keep-alive settings, and timeouts.
  - Compression and header size limits suitable for financial payloads.
- Provide baseline server properties that can be imported by services and overridden when necessary.
- Capture security hardening guidelines related to HTTP and TLS configuration (even if actual keys and certificates are managed externally).
