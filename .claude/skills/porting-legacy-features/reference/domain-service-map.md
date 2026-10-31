# Legacy domain → target service map

Rows marked **decided** were confirmed by the user. Rows without a mark follow from the existing module list. "Spans" lists REQs in other legacy files that the capability depends on: grep and read just those sections.
A **new service** means: create it with the 3-submodule split, register it in the root `pom.xml` and the root `README.md`, and pick ports that don't collide (see `CLAUDE.md`).

## Contents
- Domain files → owning service
- Cross-cutting requirements
- Boundary notes

## Domain files → owning service

| Legacy file | Capability | Owning service | Spans | Notes |
|---|---|---|---|---|
| 01 product-catalogue | instruments, product sheet, costs, ESG, target market, PAC plans, equivalent-product search | `product-service` | 08 shelf (tenant-scoped / proposal-scoped search); sheet sections from PRD-005..010, 015 | Skeleton today; migrate to the 3-submodule split first. Without the shelf, ship tenant-wide search and defer the proposal-scoped variant. |
| 08 model-portfolios (shelf part) | product shelf ("multi-catalogue"), buy lists, versioned per tenant | `product-service` | – | Shelf = which products a tenant may propose. |
| 08 model-portfolios (models part) | model portfolios, weights, suspension, reassignment | `portfolio-service` | 08 shelf | **decided**. |
| 02 portfolio-positions-valuation | positions, AS-IS / potential portfolio, valuation, performance, risk, breakdowns, movements | `portfolio-service` | 11 position / AUM feeds (REQ-ING-003 items) | Owns the position data; it receives feeds as events from data-ingestion-service. |
| 03 proposal-advisory | proposal lifecycle (FREE / GUIDED_OPTIMIZER / GUIDED_REBALANCER), wizard steps, supervision, conflict of interest | `proposal-service` | 04 order events; suitability-service | Status derived from order events. |
| 03 + 12B suitability / appropriateness / target market | rules engine, evidence numbering, re-check before execution | **new service** `suitability-service` (**decided**) | 03 PRO-006..008; 04 ORD-003 (step 3, re-check) and ORD-005; 12B. Suggested port order: PRO-006 + re-check contract, then PRO-007/008, then 12B | Covers suitability, appropriateness and target-market checks. Owns the rule set and append-only, numbered evidence. Called synchronously by proposal-service and by other bank channels. The re-check is triggered by order-service (its transaction status changes) through an event. |
| 04 order-lifecycle | transactions, dispatch, reconciliation, expiry, manual handling | `order-service` | 03 proposal events; suitability re-check | Needs the api-data / event-data split. Order state machine as a dispatcher of handlers. |
| 05 reporting-costs | PDF reports, ex-ante / ex-post costs and charges, inducements, exports | `reporting-service` | – | Builds its own read models from events. |
| 05 e-signature documents | document generation, signature, storage | `document-service` | – | Reporting asks; document-service stores and signs. |
| 06 advisor-network | advisor hierarchy, delegation, portfolios of customers | `advisor-service` | – | Identities stay in `user-service`; roles, role assignments, permissions and (later) visibility scopes live in `profiler-service` (**built**: role assignments with four-eyes, see `docs/domain/process/security/roles-and-permissions.md`). Legacy profiles (REQ-ADV-003) map to `WmpRole` values added when their features are ported. |
| 06 visibility scopes | OWN / BRANCH / MULTI_BRANCH / TENANT / ... filters | `platform-core` | – | Cross-cutting: one shared filter, scopes resolved from the JWT. |
| 07 notifications-alerts | portfolio / regulatory alerts, disclaimers, badges | `notification-service` | – | Delivery by email goes through `email-service` events. |
| 09 advisory-fees-contracts | service packages, fee schedules, discounts, billing feed | **new service** `contract-service` (**decided**) | – | Owns customer service agreements, starting with paid advisory: package or service level, fee schedule, discounts, exceptions, rebates. Emits fee and billing events. Can later host other agreements (custody, mandates). |
| 10 customer-mifid-aml | customer profile, family, financials, goals, MiFID / IDD, AML | `customer-service` | – | Gap analysis only: port the ⚠ items. |
| 11 data-ingestion-dq | feed catalogue, data-quality pipeline, upload dashboard | **new service** `data-ingestion-service` (**decided**) + `integration/<feed>` adapters | the target domain's file: for position / AUM feeds, 02 PTF-002, PTF-014, PTF-015 | data-ingestion-service owns the feed catalogue, the schedule, each run, data-quality results and the upload dashboard. The adapter only parses the vendor format (no scheduler). The target service owns the data and consumes the feed's domain events. Domain jobs (expiry, valuation) stay in their own services: this is not a generic batch service. |
| 12A outbound providers | 17 provider ports | `integration/<provider>` | – | One port interface per capability; the vendor is an adapter. |
| 12B inbound channel API | API for other bank channels | the owning service's api-data (e.g. `suitability-service`) | – | **decided**: no gateway for now. Endpoints are secured by a machine-to-machine (client-credentials) JWT scope. Never a shared "legacy-compat" service. A gateway is a later deployment concern. |

## Cross-cutting requirements (00-index)

| REQ | Target |
|---|---|
| XC-001 multi-tenancy | `platform-core`: tenant and branch in `WMPContext`, tenant-scoped base entity, tenant id in CloudEvents. **built** (`TenantScopedEntity`, `WmpTenantIdentifierResolver`, `tenantid` CloudEvents extension). |
| XC-002 / 003 per-tenant config, feature flags | **decided**: typed per-bank values in `bank-service` database tables (bank = tenant registry; one table or a set of tables per area, with history; never YAML or files); per-bank on/off features in `profiler-service` (next to its sections/features/permissions); platform-core provides a cached reader, refreshed by a `bank-settings-updated` / `feature-flags-updated` event. No new service. |
| XC-004 audit log | platform-core aspect publishes `user-action-performed`; **new service** `audit-service` (**decided**) stores and exports it. |
| XC-005 DISPOSITIVE / SIMULATED | `proposal-service` only. |
| XC-006 tenant re-parenting | saga across services, triggered by an event. COULD. |
| XC-007 purge | per-service consumer of `customer-purge-requested` / `tenant-purge-requested`. |
| XC-008 i18n of reference data | owning service's reference tables + label table. |
| XC-009 paging / export | shared `Pageable` to move from bank-api-data into platform-core `api-data.yaml` (not there yet); streamed XLSX helper. |

## Boundary notes

- If two services both seem to own a rule, the owner is the one whose aggregate changes state because of it.
- Data another service needs for display goes into a read model fed by events, never into a cross-service query.
- If a port needs a service that is still a skeleton, migrating that service to the split structure is step 0 of the port.
