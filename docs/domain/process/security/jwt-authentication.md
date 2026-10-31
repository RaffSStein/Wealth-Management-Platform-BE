# JWT authentication and tenancy

How the Wealth Management Platform issues, validates and propagates JSON Web Tokens (JWT), and how a token decides
which bank's (tenant's) data a request can see.

## Contents
1. [Overview](#1-overview)
2. [Token anatomy](#2-token-anatomy)
3. [Login: issuing a session token](#3-login-issuing-a-session-token)
4. [Validating a request in any service](#4-validating-a-request-in-any-service)
5. [Switching branch](#5-switching-branch)
6. [Registration and password setup](#6-registration-and-password-setup)
7. [Propagating the context to Kafka events](#7-propagating-the-context-to-kafka-events)
8. [Logout and revocation](#8-logout-and-revocation)
9. [Local development](#9-local-development)
10. [Known limitations and follow-ups](#10-known-limitations-and-follow-ups)

---

## 1. Overview

- **user-service is the only issuer.** It signs tokens with an RSA private key (RS256).
- **Every service verifies** tokens with the matching public key. No service calls user-service to validate a token.
- **One token = one active membership.** A user can belong to several branches of several banks; the token is issued for
  one of them and carries its bank (`tenantId`), branch (`branchCode`) and the roles held there.
- **The tenant scopes the data.** Hibernate reads `tenantId` from the request context and filters every query on
  bank-owned entities. A bank never sees another bank's rows.
- **Events carry the context.** Kafka events forward user, tenant and correlation id, so consumers act on behalf of the
  original caller and in the same tenant.

```mermaid
flowchart LR
    FE["Frontend<br/>stores one token"]
    US["user-service<br/>issuer<br/>RS256 private key"]
    SVC["any service<br/>customer, document, proposal...<br/>RS256 public key"]
    DB[("service DB<br/>rows filtered by tenant_id")]
    K[["Kafka<br/>CloudEvents with<br/>tenantid / userid / correlationid"]]
    C["consumer service<br/>restores the context"]

    FE -- "POST /auth/login" --> US
    US -- "JWT" --> FE
    FE -- "Authorization: Bearer JWT" --> SVC
    SVC -- "queries scoped to tenantId" --> DB
    SVC -- "publish event" --> K
    K -- "consume event" --> C
```

---

## 2. Token anatomy

A JWT has three dot-separated parts: header, payload (claims) and signature. Only the signature makes it trustworthy:
anyone can read the claims, so **tokens never contain secrets**.

### Claims

| Claim | Example | Set by | Read by | Meaning |
|---|---|---|---|---|
| `sub` | `5f0c7b2e-...` | user-service | — | User id (subject) |
| `jti` | `b1e4...` | user-service | revocation, password setup | Unique token id |
| `iss` | `user-service` | user-service | — | Issuer |
| `iat` / `exp` | epoch seconds | user-service | Spring resource server | Issued at / expires at |
| `userId` | `5f0c7b2e-...` | user-service | `WMPContext` | User id |
| `email` | `ada@bank.test` | user-service | `WMPContext`, JPA auditing (`created_by`) | User email |
| `roles` | `["ADVISOR"]` | user-service | Spring Security (`ROLE_ADVISOR`), `@PreAuthorize` | Roles of the **active membership only** |
| `tenantId` | `BANK_A` | user-service | `WMPContext`, Hibernate tenant filter, MDC, events | Bank of the active membership |
| `branchCode` | `BR_01` | user-service | `WMPContext`, profiler permissions, MDC, events | Branch of the active membership |
| `purpose` | `PASSWORD_SETUP` | user-service | `SecurityContextFilter`, `PasswordConfirmJwtFilter` | Present only on single-purpose tokens |

Custom claim names live in `WmpJwtClaims` (platform-core) and are shared by the issuer and every parser.

### Token types

| | Session token | Password-setup token |
|---|---|---|
| Issued by | `POST /auth/login`, `POST /auth/switch-branch` | `POST /auth/register` (sent by email) |
| Lifetime | `security.jwt.expiration-seconds` (default 3600 s) | `security.jwt.password-setup-expiration-seconds` (default 900 s) |
| `roles` / `tenantId` | yes / yes (if the user has a membership) | no / no |
| `purpose` | — | `PASSWORD_SETUP` |
| Accepted on | every authenticated endpoint | **only** `POST /auth/password/setup`, once |

---

## 3. Login: issuing a session token

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant FE as Frontend
    participant AC as user-service<br/>AuthController
    participant AS as AuthenticationService
    participant AM as AuthenticationManager
    participant DB as users / branch_users
    participant JI as JwtTokenIssuer

    U->>FE: email + password (optional branch)
    FE->>AC: POST /auth/login {email, password, bankCode?, branchCode?}
    AC->>AS: login(request)
    AS->>AM: authenticate(email, password)
    AM->>DB: load user, check bcrypt hash
    alt wrong credentials
        AM-->>FE: 401
    end
    AS->>DB: load the user's memberships (bankCode, branchCode, role)
    alt branchCode requested but not a membership
        AS-->>FE: 403 ACCESS_DENIED
    else branch requested and found
        AS->>AS: active = that membership
    else no branch requested
        AS->>AS: active = first membership (lowest id)
    end
    AS->>AS: roles = roles held in the active membership only
    AS->>JI: issueToken(sub, email, roles, userId + tenantId + branchCode)
    JI->>JI: sign RS256 with the private key, add jti, iat, exp, iss
    JI-->>AS: JWT
    AS-->>FE: 200 {token, tokenType: Bearer, expiresIn}
    FE->>FE: store the token (one at a time)
```

**A user without any membership** gets a `CUSTOMER` token with no `tenantId`. It works on user endpoints like `/me`, but
every bank-owned endpoint refuses to write (401 `MISSING_HEADER_TOKEN_FIELD [tenantId]`) and reads find nothing.

---

## 4. Validating a request in any service

Every service gets the same security setup from platform-core (`SecurityConfig`, `JwtConfiguration`). The filters
run in this order:

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant RS as Spring resource server<br/>BearerTokenAuthenticationFilter
    participant PC as PasswordConfirmJwtFilter
    participant SC as SecurityContextFilter
    participant AZ as Authorization<br/>authenticated / @PreAuthorize
    participant CT as Controller and service
    participant HB as Hibernate<br/>WmpTenantIdentifierResolver
    participant DB as Service DB

    FE->>RS: GET /customer/42<br/>Authorization: Bearer JWT
    RS->>RS: verify signature with the public key and check exp
    alt invalid signature or expired
        RS-->>FE: 401
    end
    RS->>RS: roles claim to authorities ROLE_*
    PC->>PC: only on POST /auth/password/setup:<br/>require purpose PASSWORD_SETUP
    SC->>SC: parse claims into WMPContext
    alt token store (Redis) unreachable
        SC-->>FE: 503, fail closed
    else jti revoked (shared store)
        SC-->>FE: 401
    else purpose token outside its endpoint
        SC-->>FE: 401
    end
    SC->>SC: SecurityContextHolder.setContext(WMPContext)<br/>MDC: userId, tenantId, branchCode, correlationId
    AZ->>AZ: check authentication and roles
    alt role missing
        AZ-->>FE: 403
    end
    AZ->>CT: invoke endpoint
    CT->>HB: repository query
    HB->>HB: tenant = WMPContext.tenantId
    HB->>DB: SELECT ... WHERE id = 42 AND tenant_id = 'BANK_A'
    DB-->>CT: row, or nothing if it belongs to another bank
    CT-->>FE: 200, or 404 for another bank's resource
    SC->>SC: finally: clear WMPContext and MDC
```

Points worth knowing:
- **Another bank's resource answers 404, not 403.** For the caller it simply doesn't exist.
- **Writes are stamped automatically.** Saving a `TenantScopedEntity` fills `tenant_id` from the token; a request
  without a tenant can't save one.
- **Native SQL is not filtered** by Hibernate: add `tenant_id = :tenantId` yourself, or use JPQL.
- The context lives in a `ThreadLocal`, so `@Async` work and new threads don't inherit it automatically.

---

## 5. Switching branch

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant AS as user-service<br/>AuthenticationService
    participant DB as branch_users
    participant JI as JwtTokenIssuer
    participant RV as TokenRevocationService<br/>shared Redis store

    FE->>AS: POST /auth/switch-branch {branchCode, bankCode?}<br/>Authorization: Bearer current JWT
    AS->>DB: memberships of the current user (from WMPContext.email)
    alt not a member of that branch
        AS-->>FE: 403, the current token stays valid
    end
    AS->>JI: issue a token for the new membership<br/>tenantId, branchCode, roles of that membership
    JI-->>AS: new JWT
    AS->>RV: revoke(current jti, current exp)
    AS-->>FE: 200 {token, ...}
    FE->>FE: replace the stored token
    FE->>AS: GET /me
    AS-->>FE: memberships, the new one has active = true
```

`GET /me` lists every membership and flags the one the token was issued for (`active: true`). The FE uses that
membership's role for its UI and offers the others in a branch switcher.

---

## 6. Registration and password setup

```mermaid
sequenceDiagram
    autonumber
    actor U as New user
    participant FE as Frontend
    participant US as user-service
    participant K as Kafka<br/>user-created
    participant ES as email-service
    participant PR as profiler-service
    participant PS as PasswordSetupTokenService<br/>consumes the jti in Redis

    FE->>US: POST /auth/register {email, names, phone}
    US->>US: save user WITHOUT memberships<br/>(a public signup can't grant roles or bank access)
    US->>US: issue password-setup token<br/>purpose PASSWORD_SETUP, exp 15 min, no roles
    US->>K: publish UserCreatedEvent (user, memberships, onboardingToken)
    US-->>FE: 201
    K-->>PR: create permissions per branch (none yet)
    K-->>ES: send onboarding email
    ES->>U: link: password-setup-link?token=...
    U->>FE: open the link and choose a password
    FE->>US: POST /auth/password/setup {password}<br/>Authorization: Bearer setup token
    US->>US: PasswordConfirmJwtFilter: signature, purpose, exp
    US->>US: SecurityContextFilter: purpose allowed on this endpoint
    US->>PS: validateAndConsume(token)
    alt jti already used (on any instance, also after a restart)
        PS-->>FE: error, the link works once
    else token store unreachable
        PS-->>FE: 503, the setup is refused, never accepted twice
    end
    US->>US: validate password policy, store bcrypt hash
    US-->>FE: 202
    FE->>US: POST /auth/login (section 3)
```

The setup token is **refused on every other endpoint** of every service (401). It can't be used as a session token.

---

## 7. Propagating the context to Kafka events

```mermaid
sequenceDiagram
    autonumber
    participant P as Producer service<br/>WMPBaseEventPublisher
    participant K as Kafka topic
    participant C as Consumer service<br/>WMPBaseEventConsumer
    participant H as Consumer handler
    participant DB as Consumer DB

    Note over P: request thread with WMPContext from the JWT
    P->>P: build CloudEvent, add extensions<br/>userid, email, tenantid, branchcode, correlationid
    P->>K: send
    K-->>C: deliver CloudEvent
    C->>C: WMPContext from the extensions<br/>(no JWT is involved)
    C->>H: handler(payload)
    H->>DB: writes stamped with tenant_id = producer's tenant
    C->>C: finally: clear WMPContext
```

Events don't carry the JWT itself. The consumer trusts the extensions because only platform services can publish to
the topics.

---

## 8. Logout and revocation

```mermaid
flowchart TB
    A["POST /auth/logout or /auth/switch-branch<br/>handled by user-service"] --> B["TokenRevocationService.revoke<br/>SET wmp:jwt:revoked:&lt;jti&gt; NX<br/>TTL = remaining token life + 60 s"]
    B --> R[("Redis<br/>shared by every service and instance")]
    C["same token sent to any service"] --> F["SecurityContextFilter<br/>EXISTS wmp:jwt:revoked:&lt;jti&gt;"]
    F --> R
    F -- "key present" --> D["401 revoked"]
    F -- "key absent" --> G["request continues"]
    F -- "Redis unreachable" --> H["503, fail closed"]
```

- **Revocation is platform-wide.** A token revoked by user-service is rejected by every service and instance, because
  they all check the same Redis store.
- **Entries clean themselves up.** Each key expires shortly after the token itself would, so nothing needs a cleanup
  job.
- **Fail closed.** If Redis can't be reached, authenticated requests get 503 instead of risking a revoked token being
  accepted. Requests without a token are not affected.
- The store also holds consumed single-use tokens (`wmp:jwt:setup-used:<jti>`), so a password-setup link works once.

| Namespace | Key | Written by | Read by |
|---|---|---|---|
| `revoked` | `wmp:jwt:revoked:<jti>` | user-service on logout and branch switch | `SecurityContextFilter` in every service |
| `setup-used` | `wmp:jwt:setup-used:<jti>` | `PasswordSetupTokenService` on first use (atomic `SET NX`) | the same call: a second write fails |

Clients should still discard the token on logout and branch switch.

---

## 9. Local development

### Keys and the local token
- `core/platform-core/src/main/resources/jwt/public_key.pem`: public key every service uses by default
  (`security.jwt.publicKeyPath` in `platform-shared-properties.yaml`).
- `business-modules/user-service/user-core/src/main/resources/keys/`: key pair user-service signs with.
- `core/platform-core/src/main/resources/jwt/local_token.txt`: a ready-made token without expiry:
  `tenantId=LOCAL-BANK`, `branchCode=LOCAL-BRANCH-01`, `roles=[ADMIN, SUPERVISOR]`, `email=john@doe.com`.

**These keys are for local development only.** Never use them in a shared environment.

### Signing a test token
Use a different tenant to test isolation. On Windows, run this in Git Bash, which ships with `openssl`.

```bash
cd core/platform-core/src/main/resources/jwt
b64() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
HEADER=$(printf '%s' '{"alg":"RS256","typ":"JWT"}' | b64)
PAYLOAD=$(printf '%s' '{"sub":"jane","userId":"jane","email":"jane@roe.test","tenantId":"OTHER-BANK","branchCode":"OTHER-BRANCH-01","roles":["ADVISOR"]}' | b64)
SIGNATURE=$(printf '%s' "$HEADER.$PAYLOAD" | openssl dgst -sha256 -sign private_key.pem -binary | b64)
echo "$HEADER.$PAYLOAD.$SIGNATURE"
```

Then call a service with it: `curl -H "Authorization: Bearer <token>" http://localhost:8090/customer-service/customer/<id>`.

### Configuration keys
| Key | Where | Default | Purpose |
|---|---|---|---|
| `security.jwt.publicKeyPath` | every service (`platform-shared-properties.yaml`) | `classpath:jwt/public_key.pem` | Verification key |
| `security.jwt.header` / `prefix` | every service | `Authorization` / `Bearer` | Where the token is read |
| `security.jwt.private-key-path` | user-service | `classpath:keys/private_key.pem` | Signing key |
| `security.jwt.issuer` | user-service | `user-service` | `iss` claim |
| `security.jwt.expiration-seconds` | user-service | `3600` | Session token lifetime |
| `security.jwt.password-setup-expiration-seconds` | user-service | `900` | Setup token lifetime |
| `wmp.onboarding.password-setup-link` | email-service | `http://localhost:4200/onboarding/setup-password` | Link in the onboarding email |
| `platform.security.token-store.type` | every service | `REDIS` | `REDIS` (shared) or `IN_MEMORY` (per instance, for tests or isolated runs) |
| `platform.security.token-store.key-prefix` / `clock-skew` | every service | `wmp:jwt` / `60s` | Redis key prefix; extra TTL on top of the token's remaining life |
| `spring.data.redis.host` / `port` / `timeout` | every service | `localhost` / `6379` / `2s` (env `REDIS_HOST`, `REDIS_PORT`) | Token store connection; a short timeout so a dead Redis fails fast |

---

## 10. Known limitations and follow-ups

| Limitation | Impact | Proposed fix |
|---|---|---|
| Only single tokens can be revoked | No "log out everywhere"; a password change or a disabled user doesn't invalidate tokens already issued | Per-user "tokens issued before T are invalid" timestamp in the same store, checked by `SecurityContextFilter`; build it with disable/enable user |
| Redis is a hard dependency of authenticated traffic | A Redis outage answers 503 on every authenticated call (fail closed, by design) | Run Redis highly available (replica + sentinel, or a managed service) outside local |
| One static key pair, no `kid` header | Rotating the key invalidates every token at once | Publish keys with ids (JWKS) and add `kid` to the header |
| Keys and `jwt_configuration.md` (which contains a private key) are in the repo | Fine for local development only | Load keys from a secret manager outside local profiles |
| No root tenant | Group-level users can't see several banks at once | Comes with visibility scopes (Hibernate `isRoot`) |
| The context is a `ThreadLocal` | `@Async` tasks and scheduled jobs have no tenant | Context-propagating executor and a "run as tenant" helper |
