# 12 Integrations: outbound provider catalogue and inbound API for other channels

## Purpose
This file covers two things:
- **Part A**: what the WMP **consumes**, generalized into provider ports. One adapter per vendor/tenant.
- **Part B**: what the WMP **exposes** to other bank channels: internet/mobile banking, branch order entry, CRM, and the insurance and managed-portfolio desks. Suitability-as-a-service is the most important piece.

---

## Part A: outbound provider ports

| Port (generic name) | Functional capabilities used | Legacy shape | Notes |
|---|---|---|---|
| **CoreBankingPositionsPort** | positions per dossier / per customer / pari-titled; dossier details (by id, acronym or subject); holders; authorised subjects and "on behalf of"; current accounts; contract types; PAC details; movements and movement types; orders and order statuses; MiFID indicators per subject/dossier/dispositive operation; correspondence data; "is bank active" | SOAP + mainframe transactions | the source of truth for holdings |
| **CoreBankingCustomerRegistryPort** | personal data, addresses, documents, related subjects, domicile, activity/ATECO descriptions, account type, customer data bus refresh | REST/mainframe | [IT] codes |
| **InstrumentServicePort** | instrument base/detail data, prices, venues, issuers, operation types, primary market (placements, auctions), repo, securities lending, exchange calendar, asset config, **commission & expense calculator**, **conflict-of-interest check** (plus operation types inherently in conflict for online channels) | SOAP | |
| **OrderEntryPort** | submit order (dispositive) → outcome code | SOAP | |
| **LiquidityPort** | available liquidity per account/dossier | SOAP/REST | |
| **PerformancePort** | portfolio performance (MWRR), positions opening date, average assets (TMD), domain lists, movements | SOAP | |
| **RiskAnalyticsPort** [VENDOR] | analyze portfolio, analyze total portfolio, analyze proposal (rule evaluations: suitability rules, risk, breakdowns, sleeves, contributors), generate proposal (optimizer with buy list and constraints) | REST (OpenAPI) | design for swap to in-house rules |
| **MarketDataPort** [VENDOR] | fund universe, breakdowns, performance and risk metrics; login + data output download | REST + files | |
| **CostsAndChargesPort** [VENDOR] | ex-ante/ex-post costs calculation, inducement calculation, yield, partitioned list, cost document stream; costs & charges and inducement elaboration + query | SOAP | |
| **MifidRegistryPort** | get/save the MiFID questionnaire per subject, indicators per subject(s), questionnaires to compile | SOAP | sync of profiles with core banking |
| **AmlPort** | AML risk profile and scoring per subject, potential matches, questionnaire | REST | |
| **DocumentRepositoryPort** | get document data (KID, forms, signed docs) | SOAP | |
| **ESignaturePort** [VENDOR] | compose + send for signature, by document type; outcome callback | REST | |
| **FundPlatformPort** [VENDOR] | login, generate subscription PDF, list transactions | REST | third-party fund distribution platform |
| **VisibilityConePort** [TENANT] | user visibility areas (wallet/branch cones) | REST | |
| **IdentityPort** | SSO login/token (replaced by OIDC) | REST | |
| **EmailPort** | templated emails | SMTP | |
| **FilePort** | inbound/outbound file exchange (file 11) | SFTP/share | |

Cross-cutting adapter requirements:
- Per-port timeouts: legacy uses 30 s, and 60 s for the costs engine.
- Retries with backoff for idempotent reads.
- Circuit breakers.
- Mocks per port for tests. Legacy had a mock web server per provider.
- Credentials in a secret store, never in config.
- Group-level users cannot call per-tenant core-banking services: the adapter must use the tenant's technical identity.

---

## Part B: inbound API exposed to other channels

### REQ-EXT-001 Suitability-as-a-service for orders from other channels (MUST)
Other channels (branch order entry, online banking) call the WMP to check a single new order **outside a WMP proposal**:
- **Input**: tenant, customer id/dossier, product (ISIN + currency), operation (buy/sell/switch with switch-in and switch-out), amount/quantity, price, currency.
- **Processing**:
  1. Normalize identifiers.
  2. In parallel, fetch holders/AUM and average assets from core banking.
  3. Build the AS-IS potential portfolio.
  4. Apply the operation → TO-BE.
  5. Determine the scope: the product is in/out of the advisory perimeter. For a switch, the order is in scope only if both legs are in scope.
  6. Determine the suitability case. Assume "pure suitability" (*mera adeguatezza*), i.e. a suitability check without a personal recommendation.
  7. Convert currency to base.
  8. Apply the ESG profile special handling.
  9. Call the risk engine.
  10. Store the evidence with a generated suitability ID: external IDs and internal IDs have different formats.
  11. Set the cost-benefit-analysis activation per case.
- **Output**: outcome (OK / FAILED / ERROR / BAD_REQUEST / UNAUTHORIZED / EMPTY_ESG_SECTION / TRANSACTIONS_EMPTY / ISIN_SWITCH_OUT_NOT_ALLOWED / CUSTOMER_NOT_FOUND), rule details, request id, financial class, in-perimeter flag.
- **Variants**: suitability for a managed-portfolio (GP) subscription; suitability for orders on dossiers under warranty (guarantee), plus the GP variant; suitability status query by id.
- **Mode**: asynchronous processing with status polling is supported.
- **Source flag**: `EXTERNAL` vs `WMP` on the evidence.

### REQ-EXT-002 Appropriateness-as-a-service (MUST)
- Single-order appropriateness check for non-advised channels.
- Client class × knowledge × product complexity. Evidence is stored.

### REQ-EXT-003 Target-market-as-a-service (MUST)
- Customer vs product target-market check for a single order, returning a return code.
- Async variant.

### REQ-EXT-004 Model portfolio queries for other channels (SHOULD)
- List the model portfolios for a customer, by tenant + customer id.
- Get the product list of a model portfolio.

### REQ-EXT-005 Products for warranty dossiers (SHOULD)
- Search products eligible for dossiers under guarantee, and fetch their volatility.

### REQ-EXT-006 Dossier list by network visibility (SHOULD)
- Return dossiers for a user's visibility cone (wallet/branch case), for CRM integration.

### REQ-EXT-007 Position keeping API (MUST)
For a list of customers: financial positions summary, portfolio details, position detail, YTD performance (see file 02).

### REQ-EXT-008 MiFID questionnaire and indicators API (MUST)
- By tenant + customer: return the MiFID indicators (profiles, status, expiry) and the full questionnaire.
- Used by other channels to show or check the profile.

### REQ-EXT-009 AML email trigger (COULD)
- Endpoint for another channel to trigger the AML officer notification for an operation.

### REQ-EXT-010 External auth (MUST)
- Machine-to-machine login for external callers. Legacy has a login endpoint returning a token.
- New BE: OAuth2 client credentials with scopes per capability (suitability, TM, appropriateness, positions, MiFID).
- "Relaxed" internal APIs exist that skip visibility filters for trusted callers. Model them as an explicit scope.

### Async (Part B)
- **Event**: `external-suitability-evaluated` (for audit and analytics).
- **Async pattern**: request accepted → id → result via status endpoint or callback event.

## Regulatory drivers
- **MiFID II suitability / appropriateness / TM** apply to all channels, so the WMP acts as the group's central "regulatory engine".
- **Record keeping**: for every check.

## Legacy-specific, do not port
- Special handling of ISINs for "member" (cooperative shareholder) requests. **[TENANT-POLICY]**
- Mainframe-specific ID formats, e.g. a 32-char zero-padded internal suitability ID. Generate UUIDs, and add a tenant-formatted external reference if needed.
- Customer id ↔ registry id normalization (padding).
