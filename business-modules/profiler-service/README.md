# Profiler Service

## Overview
The platform's **authorization service**. It owns:
- **role assignments**: who holds which role (CUSTOMER, ADVISOR, BANK_ADMIN, PLATFORM_ADMIN) in which bank branch,
  with the assignment rules, four-eyes approval of staff roles and revocation;
- **functional permissions**: what a role may do (sections, features, permission codes), in the database
  (`role_feature_permission`); a user's permissions are those of the role in its token, computed on each request;
- the application **sitemap** served to the FE (sections and features of an application).

user-service is the identity provider (signup, password, login). It keeps a read-only copy of the role assignments,
fed by this service's events, to put bank, branch and role in the token: login never calls profiler.
Rules and diagrams: [roles and permissions](../../docs/domain/process/security/roles-and-permissions.md);
end-to-end walkthrough: [first platform run](../../docs/domain/process/platform/first-platform-run.md).

## Module Structure
- `profiler-api-data`: OpenAPI spec `profiler-api-data.yaml` (generated REST interfaces and DTOs).
- `profiler-event-data`: event payloads (`RoleAssignmentGrantedEvent`, `RoleAssignmentRevokedEvent`, ...).
- `profiler-core`: Spring Boot application (`raff.stein.profiler`).

## Setup
- Port `8082`, context path `/profiler-service`, database `profilerdb` on `5441`.
- Seed data: `profiler-core/src/main/resources/sql/populate_config_tables.sql`: the permission catalogue (sections
  ACCOUNT, INVESTMENTS, NEWS_MARKETS, WORKSPACE mirroring the FE navigation, in display order; their features;
  permission codes) and the role permissions. Run it after the service has started once (Hibernate creates the
  tables). Idempotent; it fails on a grant naming an unknown catalogue entry. Changes take effect immediately.
- Existing databases with the old sample catalogue first run `sql/migrations/2026-10-31-align-catalogue-with-fe.sql`
  (by hand, until Flyway exists; the header gives the order).
- Calls bank-service (`wmp.clients.bank-service.url`, env `BANK_SERVICE_URL`) to check that a branch exists and is
  ACTIVE, forwarding the caller's token.
- `wmp.role-assignment.bootstrap-platform-admins` (env `WMP_BOOTSTRAP_PLATFORM_ADMINS`): emails that become
  PLATFORM_ADMIN when user-service creates them. `recent-login` (10m) and `request-ttl` (7d) tune the rules.

## Data Model
All tables are global (not tenant-scoped): a PLATFORM_ADMIN works across banks, so queries filter on the bank
explicitly.
- `role_assignments`: user, email, bank, branch, role, granted_by, approved_by, request_id (unique per user, bank,
  branch, role).
- `role_assignment_requests`: four-eyes requests (PENDING, APPROVED, REJECTED, EXPIRED), requester, decider, reason.
- `known_users`: user id and email, from `user-created` events.
- `sections`, `features` (unique per section), `permissions`: the permission catalogue; `display_order` gives the FE
  navigation order.
- `role_feature_permission`: what each role may do (role, feature, permission code). Nothing is stored per user.
- Diagram: `profiler-core/src/main/resources/documentation/E_R_Diagram.md` and
  [roles and permissions, data model](../../docs/domain/process/security/roles-and-permissions.md#12-data-model).

## Capabilities & APIs
| Method and path | Who | Purpose |
|---|---|---|
| `POST /role-assignments` | ADVISOR, BANK_ADMIN, PLATFORM_ADMIN | Assign a role: 201 immediate, 202 waiting for approval |
| `GET /role-assignments?userId=` | same | Assignments of a user visible to the caller |
| `DELETE /role-assignments/{id}` | same | Revoke; ends all the user's sessions |
| `GET /role-assignment-requests?status=` | BANK_ADMIN, PLATFORM_ADMIN | Requests of the caller's bank (all banks for PLATFORM_ADMIN) |
| `POST /role-assignment-requests/{id}/approve` / `reject` | BANK_ADMIN, PLATFORM_ADMIN | Four-eyes decision |
| `GET /permissions/getUserSitemapByClaims` | any | Permissions of the caller in the bank and branch of its token |
| `GET /sitemap/{application}` | any | Application sitemap |

## Topics & Events
| Topic | Direction | Purpose |
|---|---|---|
| `...user-service.user-created` | consumed | Records the user in `known_users`; bootstrap PLATFORM_ADMIN |
| `...profiler-service.role-assignment-granted` | produced | user-service updates its read model |
| `...profiler-service.role-assignment-revoked` | produced | user-service removes it from its read model |

Events are sent after the transaction commits. There is no transactional outbox yet: a crash between commit and send
loses the event (Phase 0 follow-up).

## Key Processes
- Assign → `role-assignment-granted` → user-service read model → the user gets the role (and its permissions) at the
  next login or branch switch.
- Revoke → assignment deleted, every token of the user revoked (shared token store), `role-assignment-revoked`.
- Permissions → `GET /permissions/getUserSitemapByClaims` reads the role permissions of the token's role(s) and
  returns sections → features → permission codes in navigation order.

## Work in Progress
- Only CUSTOMER and ADVISOR have feature permissions (catalogue and matrix: [roles and permissions](../../docs/domain/process/security/roles-and-permissions.md#the-catalogue));
  BANK_ADMIN and PLATFORM_ADMIN get no feature permissions yet (their rights come from the role in the token).
- Visibility scopes (own customers, branch, bank) from the legacy requirements will live here too.
