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

## Authentication, role assignments and tenants
user-service is the **identity provider**: signup, password, login and tokens. Who holds which role where is decided
by **profiler-service**, which owns role assignments; user-service keeps a read-only copy to build tokens.

- **Role assignments** (user + bank + branch + role) are created and revoked only in profiler-service
  (`POST/DELETE /role-assignments`). Its `role-assignment-granted` / `role-assignment-revoked` events are applied to
  `branch_users` by `RoleAssignmentGrantedEventConsumer` / `RoleAssignmentRevokedEventConsumer`
  (`RoleAssignmentReadModelService`, idempotent by `assignment_id`). Never edit `branch_users` by hand.
- `POST /auth/register` (public) and `POST /user` / `PUT /user/{id}` never create role assignments: roles sent in the body
  are ignored.
- `POST /auth/login` issues a JWT for one **active role assignment**: the requested `bankCode`/`branchCode`, otherwise
  the first one. The token carries `tenantId` (its bank code), `branchCode`, and only the roles held there. A user
  without role assignments gets a CUSTOMER token with no tenant, so they can't access bank-owned data. A new
  assignment shows up at the next login or branch switch.
- `POST /auth/switch-branch` issues a token for another role assignment of the current user and revokes the current
  token. A branch the user doesn't belong to returns 403.
- `GET /me` returns every role assignment (`userBranchRoles`, with the profiler `id` used to revoke it) and flags the
  one the token was issued for with `active: true`.
- Claim names are shared with every service through `WmpJwtClaims`, roles through `WmpRole` (platform-core).
- The password-setup token (`purpose: PASSWORD_SETUP`) is accepted only on `POST /auth/password/setup`, and only once:
  its `jti` is consumed in the shared Redis token store (platform-core), so the link can't be reused on another
  instance or after a restart.
- Logout and branch switch revoke the token in the same store; revoking a role assignment (profiler) ends every
  session of the user. Every service rejects those tokens from then on.
- **First platform run**: `wmp.bootstrap.platform-admins` (env `WMP_BOOTSTRAP_PLATFORM_ADMINS`) lists users created at
  startup if missing (`PlatformAdminUserBootstrap`); they receive the onboarding email and profiler makes them
  PLATFORM_ADMIN.
- Guides: [first platform run](../../docs/domain/process/platform/first-platform-run.md),
  [roles and permissions](../../docs/domain/process/security/roles-and-permissions.md),
  [JWT authentication](../../docs/domain/process/security/jwt-authentication.md).

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
