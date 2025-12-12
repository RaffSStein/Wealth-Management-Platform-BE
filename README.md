# Wealth Management Platform

This repository contains a modular, event-driven microservices platform for wealth management. 
Each module represents a business domain and is implemented as an independent Spring Boot microservice.

> **Note:** This repository is a work in progress. The platform is under active development and subject to frequent changes.

## Architecture
- **Event-based**: Services communicate primarily via asynchronous events using Apache Kafka as the event broker.
- **Database**: Each service uses PostgreSQL as its persistent storage. Each microservice has its own database instance, following the microservices best practice of database per service, even if all use PostgreSQL.
- **Local Development**: Docker containers are provided for all infrastructure components (Kafka, Kafka UI, PostgreSQL, Splunk, Splunk Forwarder, etc.) to simplify local development and testing. Each service and infrastructure component is configured to avoid port conflicts.
- **Logging & Monitoring**: All microservices log in JSON format to files under `/logs`, with log patterns including correlation IDs and user context. Logs are collected by a Splunk Universal Forwarder and made available in Splunk for centralized analysis. Logback configuration is shared and parametrized via Spring properties.
- **Shared Core**: Common logic, logging, and security are centralized in the `platform-core` module and imported as a dependency by all business modules.

## Modules
- [proposal-service](business-modules/proposal-service/README.md)
- [portfolio-service](business-modules/portfolio-service/README.md)
- [customer-service](business-modules/customer-service/README.md)
- [product-service](business-modules/product-service/README.md)
- [order-service](business-modules/order-service/README.md)
- [advisor-service](business-modules/advisor-service/README.md)
- [reporting-service](business-modules/reporting-service/README.md)
- [notification-service](business-modules/notification-service/README.md)
- [user-service](business-modules/user-service/README.md)
- [profiler-service](business-modules/profiler-service/README.md)
- [bank-service](business-modules/bank-service/README.md)
- [core/platform-core](core/platform-core/README.md) (shared core logic)
- [integration](integration/README.md) (external service integrations)

Each business module may contain submodules for API data models (OpenAPI-generated), event data models, and core logic.

## Maven Project Structure & POM Organization
- The root `pom.xml` is the parent for all modules and manages common dependencies, plugin versions, and properties (using variables for all versions).
- Each business module (e.g., `proposal-service`) is a Maven module with its own `pom.xml` inheriting from the parent. 
- Submodules (e.g., `proposal-api-data`, `proposal-event-data`, `proposal-core`) are defined as children in the parent module's POM and are used for API model generation, event payloads, and business logic.
- Shared dependencies (Spring Boot, Lombok, Jakarta, OpenAPI, MapStruct, etc.) are declared in the parent POM and inherited by all modules. Only module-specific dependencies are added in child POMs.
- The `platform-core` module provides shared code (logging, security, utilities) and is imported as a dependency by all business modules.

## How to Run
1. Clone the repository.
2. Start the infrastructure with Docker Compose (from the repository root, using the provided `docker-compose.yml`):
   - Unix/macOS:
     ```bash
     docker compose -f docker-compose.yml up -d
     ```
   - Windows (PowerShell or cmd):
     ```powershell
     docker compose -f docker-compose.yml up -d
     ```
     Requires Docker Desktop (or equivalent) with WSL2 backend.

3. Build all modules using the Maven Wrapper (recommended):
> Prerequisites for building and running the microservices:
> - **JDK 21** installed and configured (ensure `JAVA_HOME` and `PATH` point to a Java 21 installation).
> - **Maven Wrapper** is provided in this repository (`mvnw` / `mvnw.cmd`); a local Maven installation is not required.
   - Unix/macOS:
     ```bash
     ./mvnw clean install
     ```
   - Windows (PowerShell or cmd):
     ```powershell
     .\mvnw.cmd clean install
     ```
4. Start each microservice individually (from its module directory) using the Maven Wrapper:
   - Unix/macOS:
     ```bash
     ./mvnw spring-boot:run
     ```
   - Windows (PowerShell or cmd):
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```

## Event-Driven Approach
All business events (e.g., proposal created, order executed) are published to Kafka topics. Other services subscribe to relevant topics to react to these events, ensuring loose coupling and scalability. Event payloads are defined in dedicated submodules (e.g., proposal-event-data) and shared via dependencies.

## Technologies
- Java 21
- Spring Boot 4
- Apache Kafka
- PostgreSQL
- Docker Compose
- Splunk (for log aggregation)
- MapStruct (for mapping between entities and DTOs)
- OpenAPI (for API and event model generation)
- Lombok, Jakarta (for code simplification and modern Java EE)

## API & Event Data
- API and event payloads are defined in OpenAPI YAML files in dedicated submodules (e.g., proposal-api-data, proposal-event-data).
- Code generation is managed via Maven plugins, and shared models are imported as dependencies in other modules.

## Logging & Observability
- All microservices use a shared Logback configuration (in `platform-core`) that logs in JSON format, including correlation IDs, user IDs, and company context (populated via HandlerInterceptor and MDC).
- Log file names are parametrized per application via Spring properties.
- Splunk Universal Forwarder monitors all `.json` log files under `/logs` and forwards them to Splunk for centralized analysis.
- Timezone and log pattern are standardized across all modules.

## Security
- JWT-based authentication is implemented in the shared core and used by all modules.
- Security context and correlation IDs are propagated and logged for each request.

## DB Data Model
The data model for the Wealth Management Platform is designed to support both internal microservice autonomy and robust event-driven communication. 

- **Entity Keys**: 
  - For entities that are exposed externally (e.g., via events or APIs), a UUID is used as the primary key. This ensures global uniqueness and facilitates safe event propagation across distributed systems.
  - For entities whose lifecycle and visibility are strictly internal to a single microservice, a numeric ID is used:
    - **Long**: Used for tables that are expected to grow significantly in size (e.g., logging, audit tables).
    - **Integer**: Used for domain tables with a limited or predictable number of records.

> _This section will be expanded with detailed entity diagrams and further explanations as the data model evolves._

## Work in Progress
This repository is actively evolving. Features, modules, and documentation are subject to change as the platform matures. Please refer to the individual module [README](business-modules/proposal-service/README.md), [README](business-modules/portfolio-service/README.md), etc. for more details on each domain.
