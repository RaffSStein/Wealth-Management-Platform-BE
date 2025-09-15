# User Registration Process

## Purpose
Describe the end-to-end flow for registering a new user, from the Frontend interaction to the onboarding email dispatch, as represented in the associated UML sequence diagram.

Diagram: [User Registration Sequence Diagram](../../../assets/registration/user-registration/customer-registration/customer-registration-process-diagram.png)

![User Registration Sequence Diagram](../../../assets/registration/user-registration/customer-registration/customer-registration-process-diagram.png)

## Context and Motivation
The process separates synchronous user creation from asynchronous post-creation activities (email and future consumers) through publication of a user-created domain event on a dedicated topic, enabling decoupling and scalability.

## Actors / Components
- Frontend (CRM/BO): User interface used by an operator to input user data.
- bank-service: Provides the (filterable) list of branches (reference data).
- user-service: Validates and persists the new user; emits the domain event.
- user-created-topic: Messaging topic/queue for asynchronous propagation.
- email-service (Reporting): Consumer of the event; sends onboarding email.

---
## Extended Detailed Documentation

### 1. High-Level Narrative
The operator gathers the branch context, inputs mandatory identification data, and submits a creation request. The user-service performs authoritative validation, persists the entity in a single transaction, and (only after commit) publishes a lightweight domain event. Downstream, the email-service consumes this event and delivers an onboarding email asynchronously, ensuring the creation latency is not inflated by external side effects.

### 2. Sequence Breakdown (UML Alignment)
| # | Interaction | Purpose | Sync? | Notes |
|---|-------------|---------|-------|-------|
| 1 | Frontend -> bank-service (GET /branches) | Retrieve selectable branch references | Yes | Filters reduce payload size |
| 2 | bank-service -> Frontend (200) | Provide branch list | Yes | Empty list allowed (UI may block continue) |
| 3 | Frontend (local) | Operator fills form | Local | Client validation catches obvious errors |
| 4 | Frontend -> user-service (POST /users) | Submit creation payload | Yes | CorrelationId forwarded |
| 5 | user-service (validate) | Enforce domain & referential rules | Yes | Fail fast -> 400/409 |
| 6 | user-service (persist) | Transactional insert | Yes | Rollback on error |
| 7 | user-service -> topic (publish event) | Emit user-created | Boundary | After commit (outbox) |
| 8 | user-service -> Frontend (201) | Return created representation | Yes | Only after commit attempt |
| 9 | topic -> email-service | Deliver event | No | At-least-once semantics |
|10 | email-service (compose) | Build onboarding content | No | Fetch template / localization |
|11 | email-service -> provider | Send email | No | Retries on transient failure |

### 3. Responsibilities
- Frontend: Input collection, basic validation, correlation propagation.
- bank-service: Accurate, performant reference data exposure.
- user-service: Validation authority, transactional safety, event emission guarantee (publish-after-commit).
- user-created-topic: Reliable distribution (durable, ordered per key if required).
- email-service: Idempotent consumption, reliable email dispatch, observability of retries.



