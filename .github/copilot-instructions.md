# Repository-wide Copilot Instructions

These instructions apply to all changes across this repository. Keep them concise, practical, and always follow them unless
a file contains explicit, conflicting local guidance.

# Project Overview

This project is a back-end Spring boot powered and event-driven web application that represents a Wealth Management Platform.
With this platform users can manage their finances and portfolios, along with their goals.
It is built using Spring Boot, uses Postgres for data storage (DB) and Kafka for event management.

## Folder Structure

- `/business-modules`: Contains all the maven modules, each representing a standalone microservice.
- `/business-modules/*-service/*-api-data`: * is a placeholder which represents the name of the microservice (e.g. bank).
This folder contains the API data models (DTOs) and mappers for the respective microservice, usually in a .yaml format (Open API).
This is also a maven module that other microservices can depend on to get access to the API data models.
- `/business-modules/*-service/*-event-data`: * is a placeholder which represents the name of the microservice (e.g. bank).
This folder contains the event data models (DTOs) and mappers for the respective microservice, usually in a .yaml format (Open API).
This is also a maven module that other microservices can depend on to get access to the event data models for event-driven communication.
- `business-modules/*-service/*-core`: * is a placeholder which represents the name of the microservice (e.g. bank).
This folder contains the core business logic, services, repositories, controllers, and configurations for the respective microservice.
- `business-modules/*-service/*-core/src/main`: this folder contains the main source code for the respective microservice, as per standard Maven project structure.
- `business-modules/*-service/*-core/src/test`: this folder contains the test source code for the respective microservice, as per standard Maven project structure.
- `business-modules/*-service/*-core/src/main/java/raff/stein/*/controller`: this folder contains the REST controllers for the respective microservice.
- `business-modules/*-service/*-core/src/main/java/raff/stein/*/service`: this folder contains the service classes for the respective microservice.
- `business-modules/*-service/*-core/src/main/java/raff/stein/*/repository`: this folder contains the repository interfaces for the respective microservice.
- `business-modules/*-service/*-core/src/main/java/raff/stein/*/model`: this folder contains the JPA entity models for the respective microservice.
- `business-modules/*-service/*-core/src/main/java/raff/stein/*/config`: this folder contains the configuration classes for the respective microservice.
- `business-modules/*-service/*-core/src/main/java/raff/stein/*/event`: this folder contains consumers/producers for any interaction with some other microservice.
- `business-modules/*-service/*-core/src/main/java/raff/stein/*/exception`: this folder contains custom exceptions for the respective microservice.
- `/core`: Contains shared libraries, utilities, and common configurations used across multiple microservices.
- `/docs`: Contains documentation for the project process and more to come, including assets or any related document.
- 
## Ground rules (Required)
- Use English only for all identifiers and text in the repo: code (class/method/variable names), comments,
documentation, commit messages, PR titles/descriptions, user-facing strings, and filenames—regardless of the language used in requests or issues.
- Preserve existing conventions and style: this is a multi-module Java/Maven project (Spring ecosystem).
Match existing package structure, naming, annotations, and formatting found in neighboring code.
- Keep public APIs stable. When changing APIs (REST/OpenAPI, DTOs, events), ensure backward compatibility or clearly document
breaking changes and update all affected clients, mappers, and tests in the same change.
- Minimize blast radius: prefer the smallest coherent change set; avoid unrelated refactors in the same PR.

## Code & architecture
- Prefer standard Java, Spring Boot/Spring frameworks already used in the repo; do not introduce new dependencies unless
clearly justified and widely adopted. If added, pin versions and update the module’s pom.xml consistently.
- Follow SOLID principles and keep business logic in the appropriate module (respect the current layering and module boundaries
in business-modules/* and core/*).
- Maintain null-safety and validation: validate inputs at boundaries (controllers/services/mappers). 
Use existing validation annotations when available.

## Documentation & comments
- Keep comments accurate and succinct. Update Javadoc/KDoc/README when touching public types or endpoints.
- If you add or modify REST endpoints or DTOs, update the related OpenAPI YAML and any generated types/mappers accordingly.

## Testing & quality
- When behavior changes, add/update unit tests (and integration tests where feasible). Cover the happy path and at least one edge case.
- Prefer deterministic tests; avoid time, network, or environment flakiness unless mocked.

## Security & secrets
- Never commit secrets or credentials. Use configuration and environment variables as done elsewhere in the repo.
Sanitize logs and error messages to avoid leaking sensitive data.

## Performance & reliability
- Be mindful of allocations, blocking calls, and database interactions on hot paths. Reuse patterns already present in adjacent services.
- Add sensible timeouts and error handling for external calls; use existing resilience patterns if present.

## Shell commands & tooling
- In documentation or scripts, prefer cross-platform instructions. Where platform-specific steps are needed, include Windows cmd equivalents alongside Unix examples.

## Logging & observability
- Keep logs structured and consistent with existing services. Use the established logging framework and fields (correlationId, userId, etc.) where applicable.

## AI assistant behavior (meta)
- If requirements are underspecified, make 1–2 reasonable assumptions consistent with the repo and proceed; ask questions only if truly blocked.
- Prefer targeted diffs over large rewrites. Keep changes localized; avoid reformatting unrelated code.
- After modifying public behavior, ensure docs, OpenAPI, and tests are updated in the same change.

