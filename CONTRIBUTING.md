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
  - Build and test: `./mvnw -B -ntp clean install` (Windows: `mvnw.cmd -B -ntp clean install`)
  - Tests only: `./mvnw -ntp test`
  - Coverage reports: `<module>/target/site/jacoco/index.html`
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
A PR into `main` must pass these checks:

| Check | Workflow | Fails when |
|---|---|---|
| Conventional PR title | `pr-checks.yml` | the title is not `type(scope): subject` (types above, kebab-case scope required) |
| Dependency review | `pr-checks.yml` | the PR adds a dependency with a known high/critical vulnerability |
| Build, tests, quality gate | `maven.yml` | `./mvnw clean install` fails, or the SonarQube Cloud quality gate fails |

- CodeQL code scanning also runs (repository default setup). Address what it reports.
- Use the PR template sections: Rationale, Risk, Testing.

## Releases
Merging a PR into `main` releases automatically (`release.yml`): the build and tests run on the merge commit, then
the PR title decides the next version and a GitHub release is created with its tag. GitHub generates the notes from
the PRs merged since the previous release, so the [releases page](https://github.com/RaffSStein/Wealth-Management-Platform-BE/releases) is the changelog.

- Bump: `feat` -> minor, any other type -> patch, `!` after the scope or `BREAKING CHANGE` in the PR body -> major.
  The first release is `v0.1.0`.
- No release for PRs labelled `release:skip` or `dependencies` (Dependabot). They show up in the next release's notes.
- Versions live in git tags only. The POMs stay at `1.0-SNAPSHOT`.

### Quality gate setup (SonarQube Cloud, free for public repositories)
The Sonar step is skipped until `SONAR_TOKEN` exists, so CI stays green before the setup:
1. Sign in to [sonarcloud.io](https://sonarcloud.io) with GitHub and import `RaffSStein/Wealth-Management-Platform-BE`.
2. In the project, set *Administration > Analysis Method* to CI-based (turn off Automatic Analysis).
3. Check that `sonar.organization` and `sonar.projectKey` in the root `pom.xml` match the project.
4. Add the token as the `SONAR_TOKEN` repository secret (*Settings > Secrets and variables > Actions*).

The default "Sonar way" gate applies to new code only, including 80% test coverage on it.

## License
- Ensure changes comply with repository license terms.

## Questions
- For module-specific questions, refer to `README.md` within each service.
- For shared components, see `core/platform-core`.
