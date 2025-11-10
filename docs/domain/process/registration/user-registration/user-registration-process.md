# User Registration Process

## Purpose
Describe the end-to-end flow for registering a new user, from the Frontend interaction to the onboarding email dispatch, as represented in the associated UML sequence diagram.

Diagram: [User Registration Sequence Diagram](../../../assets/registration/user-registration/user-registration-process-diagram.png)

![User Registration Sequence Diagram](../../../assets/registration/user-registration/user-registration-process-diagram.png)

## Context and Motivation
The process separates synchronous user creation from asynchronous post-creation activities (email and future consumers) through publication of a user-created domain event on a dedicated topic, enabling decoupling and scalability.

## Actors / Components
- Frontend (CRM/BO): User interface used by an operator to input user data.
- user-service: Validates and persists the new user; emits the domain event.
- user-created-topic: Messaging topic/queue for asynchronous propagation.
- email-service (Reporting): Consumer of the event; sends onboarding email.
- profiler-service: Manages permission assignment and aggregation (roles → feature/permission set).

## Detailed Documentation

### 1. High-Level Narrative
The operator gathers the user data and submits a creation request.
The user-service performs authoritative validation, persists the entity in a single transaction,
and (only after commit) publishes a lightweight domain event.
Downstream, the email-service consumes this event and delivers an onboarding email asynchronously,
ensuring the creation latency is not inflated by external side effects.

### 2. Responsibilities
- Frontend: Input collection, basic validation, correlation propagation.
- user-service: Validation authority, transactional safety, event emission guarantee (publish-after-commit).
- user-created-topic: Reliable distribution (durable, ordered per key if required).
- email-service: Idempotent consumption, reliable email dispatch, observability of retries.
- profiler-service: Owns permission taxonomy & assignment; translates roles + branch codes into persisted feature/permission tuples; exposes query APIs.

