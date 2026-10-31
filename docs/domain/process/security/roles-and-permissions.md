# Roles, role assignments and permissions

Reference for **who may do what** on the platform: the roles, how a user gets one (role assignments), the rules for
granting and revoking them, and how they turn into functional permissions. For a hands-on walkthrough from an empty
installation see [first platform run](../platform/first-platform-run.md); for how tokens carry the role see
[JWT authentication](jwt-authentication.md).

## Contents
1. [Vocabulary](#1-vocabulary)
2. [Which service owns what](#2-which-service-owns-what)
3. [Roles](#3-roles)
4. [Who may assign what](#4-who-may-assign-what)
5. [Assigning a role](#5-assigning-a-role)
6. [Four-eyes requests](#6-four-eyes-requests)
7. [Revoking a role](#7-revoking-a-role)
8. [From role assignment to token](#8-from-role-assignment-to-token)
9. [From role assignment to permissions](#9-from-role-assignment-to-permissions)
10. [API reference](#10-api-reference)
11. [Events](#11-events)
12. [Data model](#12-data-model)
13. [Design notes and known limitations](#13-design-notes-and-known-limitations)

---

## 1. Vocabulary

| Term | Meaning | Example |
|---|---|---|
| **Role** | A platform-wide profile (`WmpRole` in platform-core) | `ADVISOR` |
| **Role assignment** | A user holds a role in one branch of one bank | Ada is `ADVISOR` at `BANK_A` / `BR_01` |
| **Active role assignment** | The one a token was issued for: its bank is the token's `tenantId`, its branch the `branchCode`, its role the `roles` claim | |
| **Role assignment request** | A staff assignment waiting for a second admin (four-eyes) | "make Ada ADVISOR at BR_01", PENDING |
| **Permission** | What a role may do: a feature plus a permission code (`role_feature_permission`) | `ORDERS / place_order` + `USE` |
| **Tenant** | A bank, identified by its bank code. Business data is scoped by it | `BANK_A` |

A user doesn't receive permissions directly: it receives **role assignments**, and the permissions follow from the
role.

## 2. Which service owns what

```mermaid
flowchart LR
    subgraph US["user-service (identity)"]
        U1["signup, password, login, /me"]
        U2[("branch_users<br/>read-only copy of role assignments")]
    end
    subgraph PS["profiler-service (authorization)"]
        P1["role assignment API<br/>rules + four-eyes"]
        P2[("role_assignments<br/>role_assignment_requests<br/>known_users")]
        P3[("role_feature_permission<br/>sections / features / permissions")]
    end
    BS["bank-service<br/>banks and branches"]
    R[("Redis<br/>token store")]
    FE["Frontend"]

    FE -- "signup, login" --> U1
    FE -- "assign, approve, revoke" --> P1
    P1 -- "branch exists and ACTIVE?" --> BS
    P1 --> P2
    FE -- "my permissions" --> P3
    P1 -- "revoke: end user's sessions" --> R
    P1 -- "role-assignment-granted / -revoked" --> U2
    U1 -- "user-created" --> P2
    U2 -- "bank, branch, role in the token" --> U1
```

- **profiler-service is the source of truth** for role assignments and permissions. All rules live there.
- **user-service keeps a copy** (`branch_users`), fed only by profiler's events, so login never calls profiler and keeps
  working when profiler is down. Nothing else writes to it.
- **bank-service** says whether a branch exists and is ACTIVE.
- **Redis** (shared token store, platform-core) lets profiler end a user's sessions in every service at once.

## 3. Roles

From least to most privileged (`raff.stein.platformcore.security.role.WmpRole`):

| Role | Scope | Typical user | Granted by |
|---|---|---|---|
| `CUSTOMER` | own data in one bank branch | a bank's client | an ADVISOR of that branch, or a BANK_ADMIN of the bank |
| `ADVISOR` | the branch's customers | private banker, advisor | a BANK_ADMIN, approved by a second BANK_ADMIN |
| `BANK_ADMIN` | one bank | the bank's administrator | a PLATFORM_ADMIN, approved by a second PLATFORM_ADMIN |
| `PLATFORM_ADMIN` | the platform (bank code `PLATFORM`) | the platform operator | **nobody**: created only by the bootstrap |

- A PLATFORM_ADMIN assignment uses the pseudo bank and branch `PLATFORM`. No business data belongs to that tenant, so
  a platform admin sees **no bank data**: its job is to create bank admins.
- Role names are parsed exactly. An unknown role stored anywhere grants nothing (there is no fallback to CUSTOMER).
- A user with no role assignment can log in but gets a `CUSTOMER` token **without tenant**: it can't read or write any
  bank data (see [JWT authentication](jwt-authentication.md), section 3).

## 4. Who may assign what

The caller's **active** role (the one in its token) decides. When a token carries several roles, the most privileged
one is used.

| Caller | May assign | Where | Effect | Recent login (≤ 10 min) |
|---|---|---|---|---|
| ADVISOR | CUSTOMER | only the bank **and branch** of its token | immediate | no |
| BANK_ADMIN | CUSTOMER | its bank, any ACTIVE branch | immediate | no |
| BANK_ADMIN | ADVISOR | its bank, any ACTIVE branch | **PENDING**, needs a second BANK_ADMIN of that bank | yes |
| PLATFORM_ADMIN | BANK_ADMIN | any bank (`bankCode` in the request) | **PENDING**, needs a second PLATFORM_ADMIN | yes |
| CUSTOMER | nothing | — | 403 | — |
| anyone | PLATFORM_ADMIN | — | 403 (bootstrap only) | — |

Rules that apply to everyone:
- **The bank comes from the caller's token.** Only a PLATFORM_ADMIN may name a bank; anyone else sending `bankCode`
  gets 403.
- **Nobody assigns, approves or revokes for themselves.** Requester and approver must be different people (compared by
  user id).
- **The branch must exist in that bank and be ACTIVE** (bank-service, looked up by bank code + branch code).
- **No duplicates**: the same user, bank, branch and role can't be assigned twice, nor requested while a request is
  pending (409).
- **Recent login** for sensitive actions: the token's `iat` must be at most 10 minutes old, otherwise
  `401 REAUTHENTICATION_REQUIRED`. The FE asks the admin to sign in again and retries. This is not a session end.

## 5. Assigning a role

```mermaid
sequenceDiagram
    autonumber
    actor A as Caller (advisor / admin)
    participant PS as profiler-service
    participant BS as bank-service
    participant K as Kafka
    participant US as user-service

    A->>PS: POST /role-assignments {userId, role, branchCode, bankCode?}
    PS->>PS: policy of the caller's role:<br/>allowed role? bank from token? own branch (advisor)?
    alt not allowed
        PS-->>A: 403 ROLE_ASSIGNMENT_NOT_ALLOWED
    end
    PS->>PS: target known? (known_users) not the caller?
    opt staff role
        PS->>PS: token iat ≤ 10 min? else 401 REAUTHENTICATION_REQUIRED
    end
    PS->>PS: no assignment or pending request yet? else 409
    PS->>BS: GET /bank/{bankCode}/branches/{branchCode}
    BS-->>PS: ACTIVE (404 / 400 otherwise, 503 if down)
    alt CUSTOMER
        PS->>PS: save role assignment
        PS-->>A: 201 {outcome: ASSIGNED, assignment}
        PS->>K: role-assignment-granted (after commit)
        K-->>US: copy into branch_users
    else ADVISOR / BANK_ADMIN
        PS->>PS: save request PENDING
        PS-->>A: 202 {outcome: PENDING_APPROVAL, request}
    end
```

The user gets the new role **at its next login or branch switch**: an existing token is never changed.

## 6. Four-eyes requests

```mermaid
stateDiagram-v2
    [*] --> PENDING: staff role requested (202)
    PENDING --> APPROVED: second admin approves<br/>(assignment created)
    PENDING --> REJECTED: second admin rejects (reason)
    PENDING --> EXPIRED: older than 7 days,<br/>found at approve/reject time
    APPROVED --> [*]
    REJECTED --> [*]
    EXPIRED --> [*]
```

| Request for | May decide | Must not be |
|---|---|---|
| ADVISOR in bank X | a BANK_ADMIN of bank X | the requester, the target |
| BANK_ADMIN of bank X | a PLATFORM_ADMIN | the requester, the target |

- Approving re-checks that the branch is still ACTIVE and that the assignment doesn't exist yet.
- The assignment records both people: `grantedBy` = the requester, `approvedBy` = the approver. The request keeps
  `requestedBy`, `decidedBy`, `decidedAt` and the rejection `reason`: it is the audit trail.
- Expiry is lazy (there is no scheduler yet): a stale request turns EXPIRED when someone tries to decide it, and that
  call answers `409 ROLE_ASSIGNMENT_REQUEST_NOT_PENDING`.
- A bank needs **two BANK_ADMINs** before it can get advisors, and the platform needs **two PLATFORM_ADMINs** before it
  can get bank admins.

## 7. Revoking a role

| Caller | May revoke | Recent login |
|---|---|---|
| ADVISOR | CUSTOMER of its own branch | no |
| BANK_ADMIN | CUSTOMER or ADVISOR in its bank | yes |
| PLATFORM_ADMIN | BANK_ADMIN | yes |

```mermaid
flowchart TB
    A["DELETE /role-assignments/{id}"] --> B{"caller's policy allows?<br/>not the caller's own role?"}
    B -- no --> X["403"]
    B -- yes --> C["delete the assignment"]
    C --> D["Redis: user-revoked-before:{userId} = now<br/>every token of the user issued until now is rejected"]
    D --> F["204, then role-assignment-revoked (after commit)"]
    F --> G["user-service removes it from branch_users"]
    D -.->|Redis down| Y["503, nothing changed"]
```

- **Every session of the user ends**, in every service, immediately: all its tokens issued up to the revocation are
  rejected with 401, including tokens for other branches. The user logs in again and gets a token for what it still
  holds.
- The revocation entry lives for the longest token lifetime (1 hour), after which every older token has expired anyway.

## 8. From role assignment to token

```mermaid
sequenceDiagram
    autonumber
    participant K as Kafka
    participant US as user-service
    participant DB as branch_users
    actor U as User

    K-->>US: role-assignment-granted {assignmentId, userId, bank, branch, role}
    US->>DB: insert, idempotent by assignment_id<br/>(a matching row from before role assignments is adopted)
    U->>US: POST /auth/login (or /auth/switch-branch)
    US->>DB: role assignments of the user
    US-->>U: JWT {tenantId: bank, branchCode, roles: [role]}
    K-->>US: role-assignment-revoked {assignmentId}
    US->>DB: delete by assignment_id
```

- `GET /me` lists the copies (`userBranchRoles`), each with the profiler `id` (to revoke it) and `active: true` on the
  one of the current token.
- Replayed or duplicated events change nothing. A grant for a user user-service doesn't know is skipped with a warning.

## 9. From role assignment to permissions

A **permission** is what a role may do: one permission code on one feature. Everything is in profiler's database, and
a user's permissions are **computed on each request**. Nothing is copied per user, so there's nothing to keep in sync.

```mermaid
sequenceDiagram
    autonumber
    actor U as User (FE)
    participant PS as profiler-service
    participant DB as profilerdb

    U->>PS: GET /permissions/getUserSitemapByClaims<br/>Bearer token {tenantId, branchCode, roles}
    alt token without bank or branch (not linked yet)
        PS-->>U: 200, no sections
    end
    PS->>DB: role_feature_permission WHERE role IN (token roles)<br/>joined with features, sections, permissions (one query)
    PS->>PS: merge roles, drop duplicates,<br/>sort by sections.display_order, features.display_order
    PS-->>U: 200 sections → features → permission codes
```

- **The role comes from the token**, the active role assignment. That keeps the answer consistent with what the
  services enforce (`@PreAuthorize` reads the same roles). A newly assigned role shows up after the next login or
  branch switch, like the token itself.
- **A change applies at once.** Granting or removing a permission is one row in `role_feature_permission`, with
  effect on the next request of every user holding that role. No restart, no rebuild.
- BANK_ADMIN and PLATFORM_ADMIN have no rows yet. Their rights come from the role in the token (`@PreAuthorize`),
  not from feature permissions. The FE has no admin section yet.

### The catalogue

The catalogue mirrors the FE navigation (`Wealth-Management-Platform-FE/src/app/layout/navigation.ts`): a **section**
is a top-bar section, a **feature** is a side-nav item. Application `WMP`. `display_order` gives the navigation order.

| # | Section | FE key | Features, in order (FE path under `/app`) |
|---|---|---|---|
| 1 | `ACCOUNT` (Account) | `account` | `HOME` (home), `INVESTOR_PROFILE` (investor-profile, investor-profile/update), `TRANSACTIONS`, `ASSETS`, `BANK_TRANSFERS`, `TAX_CREDITS`, `SECURITIES_TRANSFER` |
| 2 | `INVESTMENTS` (Investments) | `investments` | `OVERVIEW`, `POSITIONS`, `ORDERS`, `PERFORMANCE`, `ALLOCATION` |
| 3 | `NEWS_MARKETS` (Markets) | `news-markets` | `OVERVIEW`, `MARKET_NEWS`, `INDICES`, `CURRENCIES`, `COMMODITIES` |
| 4 | `WORKSPACE` (Workspace) | `workspace` | `OVERVIEW` (advisor), `CLIENTS`, `MIFID_REVIEWS`, `PROPOSALS`, `ORDERS` |

Permission codes: `VIEW` (see it), `USE` (act: place, transfer, select), `EDIT` (change data), `DOWNLOAD`.

### Who gets what (`role_feature_permission`)

| Feature | CUSTOMER | ADVISOR |
|---|---|---|
| Account: every feature | `VIEW` | `VIEW` |
| `ACCOUNT/INVESTOR_PROFILE` | `VIEW`, `EDIT` | `VIEW`, `EDIT` |
| `ACCOUNT/BANK_TRANSFERS`, `ACCOUNT/SECURITIES_TRANSFER` | `VIEW`, `USE` | `VIEW`, `USE` |
| Investments: every feature | `VIEW` | `VIEW` |
| `INVESTMENTS/ORDERS` | `VIEW`, `USE` | `VIEW`, `USE` |
| Markets: every feature | `VIEW` | `VIEW` |
| `WORKSPACE/OVERVIEW` | – | `VIEW` |
| `WORKSPACE/CLIENTS`, `WORKSPACE/ORDERS` | – | `VIEW`, `USE` |
| `WORKSPACE/MIFID_REVIEWS`, `WORKSPACE/PROPOSALS` | – | `VIEW`, `EDIT` |

That's 21 rows for CUSTOMER and 30 for ADVISOR. An advisor also gets the customer sections because the FE lets it act
on behalf of a client.

### Changing permissions

All changes are SQL against profilerdb, effective immediately:

| To... | Do |
|---|---|
| grant or remove a permission | insert into / delete from `role_feature_permission` (role, feature, permission) |
| add a feature or section | add it to `features` / `sections` with its `display_order`, then grant it |
| reorder the navigation | update `display_order` |

Keep `business-modules/profiler-service/profiler-core/src/main/resources/sql/populate_config_tables.sql` in step: it
is the source a fresh database is built from.
- **It's idempotent.** Re-running it adds what's missing and updates names and order.
- **It refuses a grant that names an unknown section, feature or permission code** (the script fails and lists the
  bad rows). The foreign keys then guarantee that every stored grant points at real catalogue entries.
- **Existing databases** get structural changes through a script in `sql/migrations/`, run by hand until the platform
  adopts Flyway or Liquibase (see the script header for the order).

## 10. API reference

Base URL `http://localhost:8082/profiler-service`; every call needs `Authorization: Bearer <token>`. Errors use the
platform `ErrorResponse` (`errorCode`, `category`, `errorMessage`, `traceId`).

| Method and path | Roles | Body / query | Success |
|---|---|---|---|
| `POST /role-assignments` | ADVISOR, BANK_ADMIN, PLATFORM_ADMIN | `{userId, role, branchCode, bankCode?}` | 201 `ASSIGNED` / 202 `PENDING_APPROVAL` (`AssignRoleResultDTO`) |
| `GET /role-assignments?userId=` | same | | 200, the user's assignments the caller may see |
| `DELETE /role-assignments/{id}` | same | | 204 |
| `GET /role-assignment-requests?status=` | BANK_ADMIN, PLATFORM_ADMIN | `PENDING` / `APPROVED` / `REJECTED` / `EXPIRED` | 200, newest first |
| `POST /role-assignment-requests/{id}/approve` | BANK_ADMIN, PLATFORM_ADMIN | | 200 `RoleAssignmentDTO` |
| `POST /role-assignment-requests/{id}/reject` | BANK_ADMIN, PLATFORM_ADMIN | `{reason}` | 200 `RoleAssignmentRequestDTO` |

| Status | `errorCode` | When | What the client does |
|---|---|---|---|
| 400 | `REQUEST_VALIDATION_ERROR` | invalid input, missing `bankCode` for a PLATFORM_ADMIN, branch not ACTIVE | fix the input |
| 401 | `REAUTHENTICATION_REQUIRED` | sensitive action with a login older than 10 minutes | sign in again, retry |
| 401 | (no body) | missing, invalid, expired or revoked token | session ended |
| 403 | `ROLE_ASSIGNMENT_NOT_ALLOWED` | the caller's role may not do this, here, or to this user | show "not allowed" |
| 404 | `OBJECT_NOT_FOUND` | unknown user, branch, assignment or request | |
| 409 | `ROLE_ASSIGNMENT_ALREADY_EXISTS` | already assigned or already pending | |
| 409 | `ROLE_ASSIGNMENT_REQUEST_NOT_PENDING` | request already decided or EXPIRED | refresh the list |
| 503 | `DEPENDENCY_UNAVAILABLE` / `TOKEN_STORE_UNAVAILABLE` | bank-service or Redis down; nothing changed | retry later |

Full contract: `business-modules/profiler-service/profiler-api-data/profiler-api-data.yaml`.

## 11. Events

| Topic | Producer | Consumers | Payload |
|---|---|---|---|
| `<env>.<tenant>.profiler-service.role-assignment-granted` | profiler | user-service | `assignmentId, userId, email, bankCode, branchCode, role, grantedBy, approvedBy` |
| `<env>.<tenant>.profiler-service.role-assignment-revoked` | profiler | user-service | `assignmentId, userId, email, bankCode, branchCode, role, revokedBy` |
| `<env>.<tenant>.user-service.user-created` | user-service | profiler (`known_users`, bootstrap), email-service | user, onboarding token |

Profiler publishes **after the transaction commits** (`@TransactionalEventListener(AFTER_COMMIT)`): a rolled-back
assignment is never announced. Schemas: `profiler-event-data.yaml`, `user-event-data.yaml`.

## 12. Data model

```mermaid
erDiagram
    KNOWN_USERS ||--o{ ROLE_ASSIGNMENTS : "holds"
    KNOWN_USERS ||--o{ ROLE_ASSIGNMENT_REQUESTS : "is target of"
    ROLE_ASSIGNMENT_REQUESTS |o--o| ROLE_ASSIGNMENTS : "approved into"
    SECTIONS ||--o{ FEATURES : "contains"
    FEATURES ||--o{ ROLE_FEATURE_PERMISSION : "granted in"
    PERMISSIONS ||--o{ ROLE_FEATURE_PERMISSION : "granted as"

    KNOWN_USERS {
        uuid user_id PK
        string email
    }
    ROLE_ASSIGNMENTS {
        long id PK
        uuid user_id
        string user_email
        string bank_code
        string branch_code
        string role
        string granted_by
        string approved_by
        long request_id
    }
    ROLE_ASSIGNMENT_REQUESTS {
        long id PK
        uuid user_id
        string bank_code
        string branch_code
        string role
        string status
        string requested_by
        string requested_by_user_id
        instant requested_at
        string decided_by
        instant decided_at
        string reason
    }
    SECTIONS {
        int id PK
        string application
        string section_code UK
        string section_name UK
        int display_order
    }
    FEATURES {
        int id PK
        int section_id FK
        string feature_code
        string feature_name
        int display_order
    }
    PERMISSIONS {
        int id PK
        string permission_code UK
        string permission_name
    }
    ROLE_FEATURE_PERMISSION {
        int id PK
        string role
        int feature_id FK
        int permission_id FK
    }
```

- `features` is unique on (`section_id`, `feature_code`), `role_feature_permission` on (`role`, `feature_id`,
  `permission_id`).
- `role` (in `role_assignments` and `role_feature_permission`) is a `WmpRole` name, not a foreign key: roles are an
  enum in code.
- The role assignments and the role permissions meet only through the role value: a user's permissions are the rows
  of the role it holds.
- user-service's `branch_users` mirrors `role_assignments` (`assignment_id` = `role_assignments.id`).

## 13. Design notes and known limitations

- **Not tenant-scoped on purpose.** These tables span banks (a PLATFORM_ADMIN works across them, and login reads every
  bank of a user), so they extend `BaseDateEntity`, not `TenantScopedEntity`. Every check filters on the bank
  explicitly, in the policies.
- **Eventual consistency.** A new role reaches the token after the event is applied, usually well under a second, and
  only at the next login or branch switch. Revocation does not wait for events: sessions end immediately via Redis.
- **No outbox yet.** A crash between commit and publish loses the event: user-service would miss a grant or a
  revocation. Phase 0 adds a transactional outbox. Until then, re-assigning repairs a missed grant, and a missed
  revocation is still enforced for sessions by Redis.
- **Lazy expiry.** Pending requests expire only when someone tries to decide them (no scheduler yet).
- **Admin permissions.** Only CUSTOMER and ADVISOR have feature permissions; admin roles get a section when the FE gets
  admin screens.
- **Visibility scopes** (own customers, branch, bank) from the legacy requirements are not built yet. They will live in
  profiler too.
