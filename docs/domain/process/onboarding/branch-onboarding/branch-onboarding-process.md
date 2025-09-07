# Bank Branch Onboarding Process

This document describes the onboarding process for a new bank branch in the Wealth Management Platform, following an
event-driven microservices' architecture. The process involves several services: bank-service, user-service, profiler-service,
and reporting-service (or email-service).

## Process Steps

1. **Frontend (CRM/BO) Data Entry**
   - The user (e.g., an operator in a CRM or back office) fills in the required data for a new bank branch, optionally including the details for a root user for the branch.

2. **API Call to Bank Service**
   - The frontend sends a POST request to the bank-service API endpoint (`/branches`) with the branch and (optionally) user data.

3. **Bank Service: Validation and Persistence**
   - The bank-service validates the branch data and, if present, the user data.
   - The branch is saved to the database.
   - An event is published to the `branch-created-topic` with the branch information.
   - If user data is present, a user creation request is published to the `create-user-topic`.

4. **User Service: User Creation**
   - The user-service consumes the user creation request from the `create-user-topic`.
   - It validates the user data and saves the new user to its database.
   - An event is published to the `user-created-topic` with the user information.

5. **Profiler Service: Permissions Assignment**
   - The profiler-service consumes the user creation event from the `user-created-topic`.
   - It assigns the appropriate permissions to the user and saves them to its database.
   - An event is published to the `permissions-created-topic` with the permissions' information.

6. **Reporting/Email Service: Notification**
   - The reporting-service (or email-service) consumes the user creation event from the `user-created-topic`.
   - It sends an onboarding email to the newly created user, confirming the successful creation of the account and providing any necessary onboarding information.

## Sequence Diagram

Below is the sequence diagram representing the onboarding process:

![Bank Branch Onboarding Sequence Diagram](/docs/domain/assets/onboarding/branch-onboarding/branch-onboarding-process-diagram.png)


---


