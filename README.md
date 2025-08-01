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
2. Start the infrastructure with Docker Compose (see the provided docker-compose.yml). This will start Kafka, Kafka UI, PostgreSQL, Splunk, and Splunk Forwarder.
3. Build all modules with Maven:
   ```
   mvn clean install
   ```
4. Start each microservice individually (from its module directory):
   ```
   mvn spring-boot:run
   ```

## Event-Driven Approach
All business events (e.g., proposal created, order executed) are published to Kafka topics. Other services subscribe to relevant topics to react to these events, ensuring loose coupling and scalability. Event payloads are defined in dedicated submodules (e.g., proposal-event-data) and shared via dependencies.

## Technologies
- Java 17
- Spring Boot 3
- Apache Kafka
- PostgreSQL
- Docker Compose
- Splunk (for log aggregation)
- MapStruct (for mapping between entities and DTOs)
- OpenAPI (for API and event model generation)
- Lombok, Jakarta (for code simplification and modern Java EE)

## API & Data Model
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

## Work in Progress
This repository is actively evolving. Features, modules, and documentation are subject to change as the platform matures. Please refer to the individual module [README](business-modules/proposal-service/README.md), [README](business-modules/portfolio-service/README.md), etc. for more details on each domain.
