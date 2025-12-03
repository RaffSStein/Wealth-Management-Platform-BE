# user-service

Technical overview and developer notes for the user-service module. This document focuses on module structure, build, configuration, and integration points. It intentionally avoids API and functional flow descriptions.

## Module structure
- Parent aggregator: `business-modules/user-service/pom.xml` (packaging: pom)
  - `user-api-data`: OpenAPI-driven DTOs and generated interfaces for REST controllers.
  - `user-core`: Spring Boot application core (controllers implement generated interfaces, services, repositories, config, models).
  - `user-event-data`: OpenAPI-driven DTOs for event payloads.

## Build & toolchain
- Java: 21 (Temurin recommended). Aligns with Spring Boot 4.
- Spring Boot: 4.x across modules.
- Maven build:
  - From repository root, build all modules:
    - `mvn -B -ntp clean install`
  - The root aggregator defines shared versions and plugins; user-service modules inherit via `<parent>`.
- CI: GitHub Actions uses JDK 21, caches Maven dependencies, and runs `clean install` + `test`.

## Generated sources (OpenAPI)
- `user-api-data` and `user-event-data` use `openapi-generator-maven-plugin` with generator `spring` and `interfaceOnly`:
  - Input specs:
    - `user-api-data/user-api-data.yaml`
    - `user-event-data/user-event-data.yaml`
  - Outputs include Java DTOs and API interfaces that `user-core` implements.
  - Key configOptions:
    - `useJakartaEe=true` (Jakarta namespaces)
    - `useBeanValidation=true`
    - `requestMappingMode=api_interface`
    - `interfaceOnly=true`
    - `useSpringBoot3=true`
    - `operationNamingConvention=camelCase`
- Regenerate by building the modules; generated code is placed under the target directories according to plugin configuration.

## Dependencies
- `user-core` depends on:
  - Spring Boot starters: `web`, `actuator`, `data-jpa`, testing (`starter-test` scope test)
  - `spring-data-commons`
  - `spring-boot-micrometer-tracing`
  - Database driver: `postgresql`
  - Mapping: `mapstruct`
  - Lombok
  - Jakarta Persistence API
  - Internal modules: `user-api-data`, `user-event-data`, and `platform-core` (shared security/config)

## Package layout (user-core)
- `raff.stein.user.controller`: REST controllers implementing generated interfaces
- `raff.stein.user.service`: business services
- `raff.stein.user.repository`: Spring Data repositories
- `raff.stein.user.model`: JPA entities
- `raff.stein.user.config`: configuration classes
- `raff.stein.user.event`: event producers/consumers
- `raff.stein.user.exception`: custom exceptions

## Configuration
- Security and OAuth2 Resource Server setup is centralized in `core/platform-core`.
- Required properties (examples; actual keys reside in application configs):
  - `security.jwt.publicKeyPath` (platform-core)
  - `security.jwt.private-key-path` (user-service, for token issuance when applicable)
- CORS/CSRF: configured for stateless APIs in platform-core, inherited by user-core.

## Database
- Postgres is the default RDBMS; JPA/Hibernate used in `user-core`.
- Ensure datasource properties are provided via environment or `application-*.yml` profiles.
- Migrations: follow project conventions (Flyway/Liquibase if present in platform-core or service modules).

## Testing
- Unit tests via `spring-boot-starter-test` in `user-core`.
- Prefer deterministic tests; mock external integrations (Kafka/events, security context).
- When public behavior changes, update tests and generated models if needed.

## Local development
- Prerequisites: Java 21, Maven.
- Typical loop:
  - Edit OpenAPI in `user-api-data.yaml` or `user-event-data.yaml` and rebuild.
  - Implement/adjust controller interfaces in `user-core`.
  - Run service with appropriate profile and environment variables for DB and security.

## Notes
- Keep identifiers, comments, and documentation in English across the repository.
- Maintain backward compatibility on public APIs; update OpenAPI specs and mappers when changes are necessary.
- Use MapStruct for DTO-domain-entity mappings; keep mapping code in dedicated `mapper` packages where applicable.
