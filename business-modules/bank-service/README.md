# bank-service

## Overview
Registry of banks (tenants) and their branches. A tenant in the platform is a bank, identified by its bank code; a
branch is identified by **bank code + branch code** (branch codes are unique only within a bank). There is no bank
table: a bank is derived from its branches. Other services use it as reference data, e.g. profiler-service checks that
a role assignment points at an existing, ACTIVE branch, and the FE fills its bank and branch dropdowns from it.

**Visibility** (`BankVisibilityPolicy`): a PLATFORM_ADMIN reads every bank; any other role only the bank of its token
(the banks it may assign roles in); a token without a bank reads nothing. Another bank's branch answers 404, and lists
simply leave other banks out.

## Module Structure
- `bank-api-data`: OpenAPI spec `bank-api-data.yaml` (generated with openapi-processor: `io.openapiprocessor.openapi.*`,
  DTOs named `*Dto`).
- `bank-event-data`: event payloads.
- `bank-core`: Spring Boot application (`raff.stein.bank`).

## Setup
- Port `8089`, context path `/bank-service`, database `bankdb` on `5443` (`postgres-bank` in `docker-compose.yml`).
- Seed data: `bank-core/src/main/resources/sql/insertBankBranches.sql`. It includes the local test branches
  `BANK_A`/`BR_01` and `BANK_B`/`BR_09` used by the local users and by
  [the first platform run guide](../../docs/domain/process/platform/first-platform-run.md).

## Data Model
- `bank_branches`: bank code, branch code (unique together), bank and branch names, SWIFT, country, bank type,
  status (`ACTIVE` ...), contact data. Reference data: extends `BaseDateEntity`, not tenant-scoped.

## Capabilities & APIs
| Method and path | Purpose |
|---|---|
| `GET /bank/banks` | Banks visible to the caller, one per bank (code, name, country, type), sorted by code |
| `GET /bank/branches` | Paged search by bank code, name, branch code, SWIFT, country, type, city, ZIP, **status**; flat `page` (zero-based), `size` (1 to 100, default 20), `sort` (`property,asc\|desc`) |
| `GET /bank/{bankCode}/branches/{branchCode}` | One branch of a bank; 404 if that bank has no such branch or it's outside the caller's scope |

Pages serialize as `{"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}` (Spring Data
`PagedModel`, `BankWebConfig`). In the spec the paging parameters are flat; openapi-processor turns them into one
Spring `Pageable` for the controller (`mapping.yaml`).

## Work in Progress
- `POST /bank/branches`, `GET/PUT/DELETE /bank/branches/{branchCode}` are declared but not implemented (they return
  no body). The single-branch operations identify a branch by branch code only, which is ambiguous across banks: use
  the bank-scoped lookup above.
- No events are published yet (`bank-created` is declared in `kafka-properties.yaml`).
