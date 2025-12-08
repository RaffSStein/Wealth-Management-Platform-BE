# Wealth Management Platform

This repository contains a modular, event-driven microservices platform for wealth management. 
Each module represents a business domain and is implemented as an independent Spring Boot microservice.

> ⚠️ **IMPORTANT:** This project is intended for educational and demonstration purposes only. It is not production-ready and should not be 
> used as-is in real-world applications without significant enhancements, security reviews, and testing.

> **Note:** This repository is a work in progress. The platform is under active development and subject to frequent changes.

## Tech stack
The list below summarizes the common technologies used across the platform. Individual sub-modules may extend this stack
with additional components to satisfy specific requirements (for example, the document service integrating with AWS or GCP storage buckets).

- Java 21
- Spring Boot 4 (Web, Data JPA, Validation, etc.)
- Spring Security (for authentication and authorization)
- Apache Kafka
- PostgreSQL
- Docker Compose
- Splunk (for log aggregation)
- Logback (for structured JSON logging)
- OpenAPI (for API and event model generation)
- Lombok, Jakarta (for code simplification and modern Java EE)


## Architecture
- **Event-based**: Services communicate primarily via asynchronous events using Apache Kafka as the event broker.
- **Database per service**: Each microservice uses PostgreSQL as its persistent storage, with an independent schema/instance per service to support loose coupling and autonomy.
- **Local Development**: Docker containers are provided for all infrastructure components (Kafka, Kafka UI, PostgreSQL, Splunk, Splunk Forwarder, etc.)
to simplify local development and testing. Each service and infrastructure component is configured to avoid port conflicts.
- **Logging & Monitoring**: All microservices log in JSON format to files under `/logs`, with log patterns including correlation
IDs and user context. Logs are collected by a Splunk Universal Forwarder and made available in Splunk for centralized analysis.
Logback configuration is shared and parametrized via Spring properties.
- **Shared Core**: Common logic, logging, security, and cross-cutting utilities are centralized in the `platform-core` module and
imported as a dependency by all business modules.
- **Modular Structure**: Each business domain (e.g., proposal management, customer management, order processing) is implemented
as a separate Maven module with its own submodules for API models, event models, and core logic.

## Modules READMEs
- [proposal-service](business-modules/proposal-service/README.md)
- [portfolio-service](business-modules/portfolio-service/README.md)
- [customer-service](business-modules/customer-service/README.md)
- [document-service](business-modules/document-service/README.md)
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
- Submodules (e.g., `proposal-api-data`, `proposal-event-data`, `proposal-core`) are defined as children in the parent module's
POM and are used for API model generation, event payloads, and business logic.
- Shared dependencies (Spring Boot, Lombok, Jakarta, OpenAPI, MapStruct, etc.) are declared in the parent POM and inherited by all modules. 
Only module-specific dependencies are added in child POMs.
- The `platform-core` module provides shared code (logging, security, utilities, etc.) and is imported as a dependency by all business modules.

## How to Run

This repository contains multiple microservices. The steps below cover the common setup; for service-specific configuration
(profiles, ports, external integrations, feature flags), refer to each module's dedicated README.

1. Clone the repository.
2. Start the infrastructure with Docker Compose (see the provided `docker-compose.yml`). This will start Kafka, Kafka UI, PostgreSQL, Splunk, and Splunk Forwarder.
   ```powershell
   docker compose up -d
   ```
3. Build all modules with Maven from the repository root:
   ```powershell
   mvn clean install
   ```
4. Start each microservice you need to run, from its module directory (e.g., `business-modules/user-service/user-core`):
   ```powershell
   mvn spring-boot:run
   ```
5. For additional configuration (active Spring profiles, environment variables, external integrations like AI providers, storage backends, etc.), see the corresponding module README:
   - [user-service](business-modules/user-service/README.md)
   - [customer-service](business-modules/customer-service/README.md)
   - [document-service](business-modules/document-service/README.md)
   - and all the other module READMEs listed above.
