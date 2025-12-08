# user-service

Technical overview for the user-service module. This document focuses on module structure, build,
configuration, and integration points. It intentionally avoids API and functional flow descriptions.

## Module structure
- Parent aggregator: `business-modules/user-service/pom.xml` (packaging: pom)
  - `user-api-data`: OpenAPI-driven DTOs and generated interfaces for REST controllers.
  - `user-core`: Spring Boot application core (controllers implement generated interfaces, services, repositories, config, models).
  - `user-event-data`: OpenAPI-driven DTOs for event payloads.

## Build & Run
- Use project standard Maven build (from repository root):
  ```powershell
  mvn clean install
  ```
- To run the service (from `user-core` directory):
  ```powershell
    mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.application.name=user-service"
  ```


## Additional dependencies
- `user-core` depends on:
  - `core/platform-core` for shared logic and common/centralized dependencies (logging, security, utilities, etc.).
  - `user-api-data` and `user-event-data` for DTOs and interfaces, as per project structure.

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
> ⚠️ WORK IN PROGRESS: Testing strategies and examples to be added.