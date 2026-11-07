# Legacy WMP feature extraction: index and cross-cutting requirements

Source: the legacy WMP backend (Java 8 / Spring Boot 2.2 / Oracle). It was built for a single Italian banking group made up of many member banks.
Purpose: a functional baseline for porting into the new bank-agnostic, event-driven WMP BE.
Everything here describes **behaviour**. No code, URLs, hostnames, credentials or customer data were copied.

## Tags used throughout
- **[IT]**: Italian-jurisdiction rule. Should become a jurisdiction plugin.
- **[TENANT-POLICY]**: bank-group or bank-specific rule. Should become configurable per-tenant policy.
- **[VENDOR]**: tied to a specific third-party provider. Should go behind a provider port/adapter.
- **MUST / SHOULD / COULD**: porting priority as I suggest it. The BE plan may override it.

## Files (in the BE's priority order)
| # | File | Domain |
|---|------|--------|
| 01 | `01-product-catalogue.md` | Instruments, product sheet, filters, PAC plans, target market, costs, ESG, equivalent-product search |
| 02 | `02-portfolio-positions-valuation.md` | Position keeping, AS-IS/potential portfolio, valuation, performance, risk, asset-allocation breakdowns, movements |
| 03 | `03-proposal-advisory-suitability.md` | Proposal lifecycle (free / optimizer / rebalancer), suitability and appropriateness, conflict of interest, supervision |
| 04 | `04-order-lifecycle-execution.md` | Transactions/orders, execution dispatch, status reconciliation, validity expiry, manual handling |
| 05 | `05-reporting-costs-documents.md` | PDF reports, costs and charges ex-ante/ex-post, inducements, data extractions, Excel exports, e-signature documents |
| 06 | `06-advisor-network-security.md` | Advisor hierarchy, roles/profiles, data-visibility scopes, delegation, SSO |
| 07 | `07-notifications-alerts.md` | Portfolio/regulatory alerts, one-time alerts, notifications, disclaimers, order-desk badges |
| 08 | `08-model-portfolios-investment-center.md` | Model portfolios, product shelf ("multi-catalogue"), buy lists, model suspension and reassignment |
| 09 | `09-advisory-fees-contracts.md` | Advisory service packages, fee schedules, discounts, exceptions, rebates, fee feeds |
| 10 | `10-customer-mifid-aml-gap.md` | Customer profile, family/financials/goals, MiFID & IDD questionnaires, AML (**gap analysis only**) |
| 11 | `11-data-ingestion-dq-dashboard.md` | Batch feed catalogue, data-quality pipeline, back-office upload dashboard |
| 12 | `12-integrations-external-api.md` | Outbound provider catalogue (generalized) and the inbound API exposed to other bank channels |

## Cross-cutting requirements (apply to all services)

### REQ-XC-001 Multi-tenancy (MUST)
The legacy system is "multi-bank inside one group": almost every record carries a bank code, and many carry a branch code.
- Every business entity MUST be tenant-scoped: tenant = bank or legal entity, optionally with a parent group.
- Branch MUST be a first-class org unit under the tenant, because visibility and reporting slice by branch.
- Some users span multiple tenants (group-level "super" users). See file 06.
- Tenant lifecycle: a tenant can be active or inactive. Mergers and acquisitions between tenants are a real business event; the legacy system has dedicated batch jobs that move customers, dossiers, disclaimers and metrics between banks. See REQ-XC-006.

### REQ-XC-002 Per-tenant configuration store (MUST)
Legacy keeps a versioned "master configuration" table: JSON parameter blobs grouped by area (technical, customer, product, proposal, diagnosis, onboarding, dashboard, third-party fund platform). The new BE needs:
- Typed, versioned config per tenant and per area, with active/inactive flag and audit of changes.
- Examples of values that live there: proposal validity windows (days per status), MiFID questionnaire duration (months, per bank), percentage-deviation thresholds, parallel upload limits, file size and extension limits, feature toggles (e.g. "model-portfolio update feature active"), group-level super-user bank codes.
- Plus a simple key/value property service readable by the FE (legacy exposes "get property by name").

### REQ-XC-003 Feature toggles per tenant (SHOULD)
Several capabilities are bank-switchable in legacy: massive onboarding, third-party fund platform, model-portfolio forced update, advanced (paid) advisory. Model them as tenant feature flags.

### REQ-XC-004 Audit log (MUST)
- Legacy writes an audit log of user actions: who, what function, which customer or dossier, when.
- A batch job exports it to the group's compliance systems.
- Must be preserved and must be exportable.

### REQ-XC-005 Operating modes: dispositive vs simulated (MUST)
Every user function exists in two flavours:
- **STANDARD/DISPOSITIVE**: real; persists and can lead to orders.
- **SIMULATION**: what-if; no orders, no regulatory records.

Proposals carry an `operativityMode` (DISPOSITIVE | SIMULATED). The simulation mode has its own report type ("proposal simulation").

### REQ-XC-006 Tenant merger/acquisition migration (SHOULD, [TENANT-POLICY])
When bank A merges into or sells branches to bank B, the system must re-key customers, dossiers, MiFID data, AUM, ex-post metrics, disclaimers and target-market reports from A to B. It must:
- keep a history table
- raise disclaimers to advisors ("customer moved from advanced to basic advisory due to merger/acquisition", see alert codes)
- deactivate obsolete disclaimers.

Generalize this as a "tenant/branch re-parenting" admin process.

### REQ-XC-007 Data retention and purge (SHOULD)
A dedicated "clean bank" job deletes all data of a bank by creation date. It covers customer, family, assets, proposals, transactions, questionnaires, documents, portfolios and alerts. In the new BE: a GDPR-style per-tenant/per-customer purge with retention rules.

### REQ-XC-008 i18n of reference data (SHOULD)
Most lookup tables have `_LANG` companions holding labels per language. Examples: family member types, professions, nationalities, sports, alert codes, asset-class levels, expenditure/income types and proposal status details. Keep label translation in reference data.

### REQ-XC-009 Pagination, sorting, Excel export (MUST)
Nearly every advisor list (customers, proposals, transactions, alerts, movements, commission rebates) supports:
- server-side paging and sorting
- multi-criteria filters
- an Excel (XLSX) export of the filtered set, sometimes PDF

Large exports are streamed.

### REQ-XC-010 Health, error logging from FE (COULD)
- Health check endpoint.
- An endpoint where the FE posts client-side errors to be logged server-side with user context.

## Legacy-specific, do not port (global)
- The custom SSO servlet stack (session token polling/refresh, debug endpoints). Replace with standard OIDC/JWT from platform-core.
- Mock web servers for providers. Replace with Postman/WireMock contract mocks.
- Bank-code-specific hard-coded branches, e.g. "bank code X is the group's super-bank" and "branch codes ≥ N are group-level branches".
- Proprietary record formats ("record 36/57" buffers, fixed-width files) and mainframe transaction codes. They are only described functionally in file 12.
- Oracle-specific SQL migration scripts per environment.

## Security note for our user (not for porting)
The legacy main `application.yml` contains plaintext client secrets and basic-auth credentials for internal services. They must not be carried over. The legacy repo owners may want to rotate them.
