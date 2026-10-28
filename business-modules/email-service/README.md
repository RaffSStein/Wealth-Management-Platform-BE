# Email Service

## Overview
The Email Service is the microservice of the Wealth Management Platform responsible for sending transactional emails
to platform users. It reacts to domain events published by other services (e.g. user creation), renders HTML emails
from Thymeleaf templates, and delivers them through a configurable SMTP server.

---

## Module Structure
- Parent aggregator: `business-modules/email-service/pom.xml` (packaging: pom)
  - `email-api-data`: OpenAPI-driven DTOs and generated interfaces for REST controllers.
  - `email-core`: Spring Boot application core (controllers, services, event consumers, templates, configuration).
  - `email-event-data`: OpenAPI-driven DTOs for event payloads.
- `email-core` also depends on `user-event-data` (to consume user events) and `platform-core` (shared security, messaging and logging).

---

## Setup

### Prerequisites
- Java 21
- Maven (or the Maven Wrapper provided at repository root)
- Docker (for local development)

### Local Infrastructure
The following containers from the root `docker-compose.yml` are required:
- `postgres-email`: PostgreSQL database `emaildb`, exposed on port `5445`.
- `mailhog`: local SMTP server on port `1025`, with a Web UI on [http://localhost:8025](http://localhost:8025) to inspect sent emails.
- Kafka broker, to consume events from other services.

### Configuration
The service runs on port `8092` with context path `/email-service`. Main properties in `email-core/src/main/resources/application.yaml`:
- `spring.datasource.*`: PostgreSQL connection.
- `spring.mail.*`: SMTP server settings (host, port, credentials, auth/starttls).
- `wmp.onboarding.password-setup-link`: frontend URL used in the onboarding email. The onboarding token is embedded
  either by replacing a `{token}` placeholder or by appending a `token` query parameter.

Kafka topics are configured in `kafka-properties.yaml`; shared properties are imported from `platform-shared-properties.yaml` (platform-core).

---

## Data Model
The service does not define any persistent entity yet. A datasource is configured to support future features
(e.g. email delivery tracking).

---

## Capabilities & APIs

### Main Capabilities
- **Onboarding Email**: Sends a welcome email to newly created users, including a link to set up their password.
- **Template Rendering**: Builds HTML email bodies using Thymeleaf templates located in `email-core/src/main/resources/templates`.

### Main APIs
No REST endpoints are exposed yet. `EmailController` is in place, and the OpenAPI specification in
`email-api-data/email-api-data.yaml` currently defines no paths.

---

## Topics & Events

The service is event-driven and interacts with the following Kafka topics:

### Consumed
- `user-created` (from `user-service`) — Triggers the onboarding email. The consumer can be toggled with
  `kafka.topics.user-service.user-created.enabled`.

### Produced
- None yet. Event payloads will follow the schemas defined in `email-event-data`.

---

## Key Processes

### Onboarding Email
1. `user-service` publishes a `UserCreatedEvent` on the `user-created` topic.
2. `UserCreatedEventConsumer` extracts the user's email, first name and onboarding token from the event.
3. `EmailService` builds the password setup link, renders `onboarding-email-template.html` and sends the email via SMTP.
4. Delivery failures are logged and do not stop event consumption.

---

## Miscellaneous
- Logging follows the platform's structured JSON pattern (correlation IDs, user context) and integrates with Splunk.
- Security and shared messaging components (e.g. `WMPBaseEventConsumer`) are provided by `platform-core`.

---

## Work in Progress
This service is under active development. Features and APIs may change. For contributions, refer to the main repository README and module-specific documentation.
