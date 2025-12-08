# Contributing Guidelines

Thank you for considering a contribution to RaffStein's Wealth Management Platform!
This document outlines the process and expectations for contributions across all modules.
> **Important:** Before starting contributions, please read
> [Core module README](core/platform-core/README.md) for a better understanding of shared components.

## Ground rules
- Always use English for identifiers, comments, documentation, commit messages, and filenames.
- Preserve existing conventions and style: multi-module Java/Maven (Spring ecosystem). Match package structure, naming, annotations, and formatting.
- Keep public APIs stable. If APIs (REST/OpenAPI, DTOs, events) change, ensure backward compatibility or document breaking changes and update all affected clients, mappers, and tests in the same change.
- Minimize blast radius: prefer the smallest coherent change set; avoid unrelated refactors.
- Write clear, maintainable code. Prioritize readability over cleverness.
- If working on core module, ensure changes do not negatively impact dependent business modules. Refer to [core/platform-core](core/platform-core/README.md) for further details.


## Repository structure
- `business-modules/*-service` modules:
  - `*-api-data`: OpenAPI DTOs and generated interfaces.
  - `*-core`: service core (controllers, services, repositories, models, configs).
  - `*-event-data`: event DTOs.
- `core/platform-core`: shared libraries, utilities, and common configurations.
- `docs`: functional documentation about product features and architecture (put any process-related docs in there, like UML diagrams).

## Development environment stack
- Java 21 (Temurin recommended), Maven.
- An IDE like IntelliJ IDEA or Eclipse (your choice).
- Docker for local databases/services. Use `docker-compose` file to spin up dependencies.
- Postman or similar for API testing.

## Build & tests
- From repository root:
  - Build: `mvn -B -ntp clean install`
  - Test: `mvn -ntp test`
- Validate changes with unit tests (and integration where feasible).
Cover happy path and at least one edge case. Prefer deterministic tests; mock external integrations.

## OpenAPI and generated code
- Update OpenAPI specs under `*-api-data/*.yaml` and `*-event-data/*.yaml` when adding/modifying endpoints or events.
- Regenerate by building the modules. Controllers in `*-core` must implement generated interfaces.
- Use MapStruct for DTO-domain-entity mapping; keep mappers in dedicated `mapper` packages.

## Code quality & security
- Avoid committing secrets. Use environment variables/config files per module.
- Sanitize logs and errors.
- Performance: be mindful of allocations, blocking calls, and DB interactions on hot paths.
- Logging: follow existing structured logging conventions (correlationId, userId, etc.).

### DB Data Model Guidelines
The data model for the Wealth Management Platform is designed to support both internal microservice autonomy and robust event-driven communication.

- **Entity Keys**:
    - For entities that are exposed externally (e.g., via events or APIs), use UUIDs as the primary keys. This ensures global uniqueness and facilitates safe event propagation across distributed systems.
    - For entities whose lifecycle and visibility are strictly internal to a single microservice, uaw a numeric ID:
        - **Long**: for tables that are expected to grow significantly in size (e.g., logging, audit tables).
        - **Integer**: for domain tables with a limited or predictable number of records.

## Git workflow
- Create feature branches from `main`.
- Keep commits small and atomic.
- Commit messages follow a conventional, type-based format:
  - `feat(<scope>)`: a new feature (user-facing or API-level).
  - `fix(<scope>)`: a bug fix.
  - `docs(<scope>)`: documentation-only changes.
  - `refactor(<scope>)`: internal refactors that do not change external behavior.
  - `test(<scope>)`: add or update tests only.
  - `chore(<scope>)`: maintenance tasks (dependencies, build, tooling, housekeeping).
- Use an imperative, descriptive subject in English, for example:
  - `feat(platform-core): add shared async task executor`
  - `fix(customer-service): handle missing customer profile`
- Prefer including the module/service in the scope when relevant (for example, `platform-core`, `customer-service`, `proposal-service`).
- In PR descriptions: include rationale, risk, testing notes; link to related docs or tickets.

## CI & PRs
- GitHub Actions builds and tests with JDK 21.
- Code scanning and AI PR assistance may be enabled at repository/organization level. If present, ensure your PR follows the guidelines and addresses reported findings.

## License
- Ensure changes comply with repository license terms.

## Questions
- For module-specific questions, refer to `README.md` within each service.
- For shared components, see `core/platform-core`.
