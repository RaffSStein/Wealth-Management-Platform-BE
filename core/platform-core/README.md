# platform-core module

`platform-core` is the shared core library for all Wealth Management Platform (WMP) microservices. It centralizes cross-cutting concerns (error handling, logging, tracing, messaging abstractions, security configuration, shared properties) so that business services can stay focused on domain logic.

## Architecture and design goals

- Provide a single, consistent implementation for platform-level concerns across all `*-service/*-core` modules.
- Minimize duplication of boilerplate (exception mapping, logging filters, Kafka wiring, shared configuration).
- Be opinionated but overridable: expose sensible defaults while allowing service-level customization.
- Support production-grade observability, resiliency, and traceability for a distributed banking environment.
- Keep `platform-core` free of business/domain logic; only platform and infrastructure concerns belong here.

## Module and package overview

> Package names and class names below reflect the current structure and may evolve. Use this section as a high-level map.

- `raff.stein.platformcore.exception`
  - Shared exception hierarchy and error codes.
  - Global exception handlers that translate exceptions into OpenAPI-compatible error responses.
- `raff.stein.platformcore.logging`
  - HTTP request/response logging filter and shared logging conventions.
- `raff.stein.platformcore.messaging.consumer`
  - Base abstractions for Kafka consumers and event handling.
- `raff.stein.platformcore.messaging.publisher`
  - Base abstractions for Kafka publishers and shared `EventData` envelope.
- `raff.stein.platformcore.messaging.consumer.config`
  - Shared Kafka consumer configuration and security context initialization for event-driven flows.
- `src/main/resources`
  - `platform-shared-properties.yaml`: shared Spring and security properties.
  - `kafka-shared-properties.yaml`: shared Kafka connection and producer/consumer properties.
  - `logback-spring.xml`: shared logging configuration.
  - `jwt/`: JWT key material and configuration documentation.
  - `api-data.yaml`: shared OpenAPI models (for example, `ErrorResponse`).

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

## Work in progress: optimistic locking and retry

Optimistic locking using `@Version` is already supported at entity level in several services. `platform-core` will standardize its handling and error mapping.

Planned directions:

- Define clear usage guidelines for `@Version` on JPA entities, possibly via a shared base class for versioned entities.
- Standardize mapping of `OptimisticLockException` (and similar persistence exceptions) to `VersionLockingException` and HTTP 409 Conflict responses.
- Provide optional retry helpers or patterns (for example, wrappers or annotations based on Spring Retry) for idempotent write operations:
  - Recommended only for operations that can be safely retried.
  - Configurable backoff and max attempts.


## Work in progress: shared `TaskExecutor` and `@Async`

To support concurrent processing without blocking HTTP request threads, `platform-core` will expose a shared `TaskExecutor` and patterns for `@Async` usage.

Planned directions:

- Provide a common `ThreadPoolTaskExecutor` bean with sensible defaults (core pool size, max pool size, queue capacity, thread name prefix, rejection policy).
- Document how to reference it via `@Async("taskExecutor")` in service components.
- Ensure that Micrometer tracing context is propagated into async threads, so logs and metrics remain correlated.
- Recommend use cases for `@Async` versus message-driven processing with Kafka (for example, short-lived background tasks vs. durable, event-driven workflows).

In the interim, services can define their own executors, but should follow a consistent naming and sizing strategy that fits platform guidelines.

## Work in progress: database throughput and shared tuning

Database throughput is a key factor in a high-throughput banking platform. `platform-core` will provide shared patterns and, where appropriate, shared configuration for Postgres data access.

Planned directions:

- Centralize recommended HikariCP datasource properties (for example, connection pool size, max lifetime, timeouts) via shared configuration files.
- Document guidance for service-level overrides based on workload characteristics.
- Align transaction timeouts and connection pool settings to avoid resource exhaustion and deadlocks.
- Encourage best practices such as:
  - Pagination for large queries.
  - Batching for bulk writes.
  - Avoiding long-running transactions and unnecessary locks.

Services should already ensure that queries are efficient, indexed, and scoped appropriately. Future versions of `platform-core` will make it easier to apply consistent DB tuning across all modules.

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

