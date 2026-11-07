# 03 Proposal / advisory and suitability

## Purpose
This is the heart of the WMP. An advisor builds an investment **proposal** (a personal recommendation) for a customer's dossier, as a list of buy/sell/switch/PAC transactions. The system:
1. runs regulatory checks: suitability, appropriateness, target market, conflict of interest, costs
2. collects the order details ("enrichment")
3. produces signable documents
4. on signature, hands the transactions to the order lifecycle (file 04)

There are three ways to build a proposal: **free** (manual), **guided via optimizer** and **guided via rebalancer**.

## State machines

### Proposal status (macro → detail)
| Macro status | Detail statuses |
|---|---|
| DRAFT | DRAFT |
| DEFINITIVE | DEFINITIVE (suitability passed, before customer confirmation) |
| CONFIRMED | IN_ENRICHMENT_DATA → PENDING_APPROVAL (supervisor) → PENDING_DELIVERY_DOCUMENTS → PENDING_SIGNATURE |
| SIGNED | EXECUTION_IN_PROGRESS, LIQUIDITY_ERROR, MARKET_CLOSED, PENDING_APPROVAL, MANUAL_EXECUTION, OPERATIONS_NOT_SUPPORTED |
| EXECUTED_PARTIALLY | (generic), TECHNICAL_ERROR, SUITABILITY_FAILED, AML_VERIFICATION_FAILED |
| EXECUTED | EXECUTED; also "WMP operation executed" for operations performed outside the custodian, e.g. on a third-party fund platform |
| DECLINED | MISSING_SIGNATURE_CONTRACT, TOTALLY_NOT_EXECUTED |
| REJECTED_BY_CLIENT | REJECTED_BY_CLIENT; also "WMP operations not executed" |
| EXPIRED | EXPIRED, EXPIRED_NOT_APPROVED |
| DELETED | (soft delete) |

- The proposal macro status is **derived** from the statuses of its transactions (file 04) after every execution or reconciliation pass.
- Every status change records `lastUpdateStatusDate`, and a status-history/action log is kept.

### Proposal steps (wizard position, persisted)
MODEL_PORTFOLIO_SELECTION → PORTFOLIO_CONFIGURATION → OPTIMIZE_PORTFOLIO | REBALANCE_PORTFOLIO → PRODUCTS → TRANSACTION_LIST → DATA_ENRICHMENT → SIGNATURE.

The backend validates that each update moves to an allowed next step (REQ-PRO-012).

### Validity windows ([TENANT-POLICY], configurable days)
The validity clock starts on the date suitability was last verified.

| Status | Expires after (configurable) |
|---|---|
| DEFINITIVE | N days |
| CONFIRMED_IN_ENRICHMENT_DATA | N days |
| CONFIRMED_PENDING_SIGNATURE | N days |
| other | no expiry |

A daily job expires proposals past their window and sets all their transactions to EXPIRED (file 04).

## Requirements

### REQ-PRO-001 Proposal header (MUST)
A proposal has:
- **identity**: number (human-readable), customer, dossier, portfolio
- **people**: advisor (author), optional supervisor, operator (for delegates)
- **type and mode**: FREE / GUIDED_OPTIMIZER / GUIDED_REBALANCER, with mode FREE | GUIDED
- **status**: status detail and current step
- **models**: model portfolio (current) and new model portfolio (if changed)
- **operativity**: type and mode (dispositive/simulated), plus any limitation (NONE | ONLY_DISINVESTMENTS) with reasons
- **flags**: delegate-subscription-power flag, supervisor approval flag
- **regulatory evidence numbers**: recommendation number, adequacy-assessment number, appropriateness-assessment number
- **guided parameters**: maximum turnover, min ticket (CTV), adherence method (to MODEL_PORTFOLIO or to CLIENT_PORTFOLIO)
- **reserves / liquidity allocations** from the needs analysis

### REQ-PRO-002 Pre-creation check (MUST)
Before creating a proposal, check and return any blocking or limiting conditions:
- **MiFID profile**: valid and not expired. If expired, block. If the ESG section is empty, limit to ONLY_DISINVESTMENTS. This is **[TENANT-POLICY]**: either block always, or only for non-joint accounts.
- **Fictitious dossier** → ONLY_DISINVESTMENTS.
- **Account holder checks**: who is the legal representative for sole proprietorships, and whether the operator is allowed.
- **Legal guardian / support administrator** present: flag it, because the document set is different.
- **Concurrent proposals**: positions moved by other open proposals are locked.
- **Bank status**: the tenant must be ACTIVE to sign.

### REQ-PRO-003 Free proposal (MUST)
The advisor edits the TO-BE product list: changes CTV or quantity of existing positions, adds new products, or removes positions (a total sell). The system then:
- **derives transactions by difference** between AS-IS and TO-BE, on CTV or quantity depending on the product type
- **auto-disinvests** AS-IS positions missing from the TO-BE list, except non-WMP products and positions locked by other proposals
- **validates per product**:
  - quantity is a multiple of the lot and ≥ the minimum
  - minimum investable amount (initial vs subsequent subscription)
  - PAC rules
  - funds: switch-causal validity
  - the product must be administered
  - primary-market product: within the placement window and plafond
- **matches switches**: pairs a disinvestment and an investment between funds of the same SICAV/company with equal CTV into a switch operation (switch-out / switch-in)
- **sets value dates** per the liquidity rules (file 02)
- **sets income distribution choice** where the fund allows it (default: reinvest)
- **checks liquidity**: investments ≤ available liquidity (current account + sells + new money + surplus)

### REQ-PRO-004 Guided proposal: optimizer (MUST)
1. Select or confirm a model portfolio (file 08).
2. Configure portfolio parameters: max turnover, min ticket, adherence method, locks per position (full/partial), and liquidity to add or keep.
3. Call the **portfolio optimizer** ([VENDOR] risk engine "generate proposal"). Inputs: the AS-IS positions, a buy-list of model products with prices and min quantities, constraints from previous runs, and the customer's risk profile. It returns proposed changes, a resulting portfolio, rule evaluations and a portfolio validity score.
4. Round changes to avoid liquidity surplus, validate quantities, and build transactions. These are changes on existing products, or new products from the model.
5. Auto-create switches between funds where possible.
6. Return proposed transactions plus suitability analytics: proposed vs original risk and allocation comparisons.

**Default locks** are positions the optimizer must not move. All are **[TENANT-POLICY]** toggles:
- products with anti-dilution commissions
- insurance products of the home group
- pledged products
- products from previous proposals
- placed investment certificates
- funds of multi-partner SICAVs
- products moved in the last month
- deposit certificates (rebalancer)

Managed multi-line mandates are exploded into sub-positions for the optimizer and re-aggregated afterwards. The last optimizer parameters used for the customer are remembered and pre-filled.

### REQ-PRO-005 Guided proposal: rebalancer (MUST)
A deterministic in-house algorithm that brings the portfolio back to the model portfolio weights, without calling the optimizer:
- **invest/disinvest** products present in both model and real portfolio towards their target weight
- **invest** products only in the model
- **disinvest** products only in the real portfolio
- **respect locks**: compute locked vs unlocked CTV, and treat unusable CTV (pledged, manual locks)
- **PAC funds**: round to a multiple of the PAC instalment
- **filter invalid transactions** (min quantity etc.). If a disinvestment is invalid, re-run without it so liquidity is not over-allocated.
- **warnings** with codes for anything skipped
- **analytics**: rebalanced portfolio analysed by the risk engine for the suitability preview

### REQ-PRO-006 Suitability verification (MUST)
Run on save to DEFINITIVE and on demand.
- **Inputs**: AS-IS potential portfolio, TO-BE portfolio, the customer's MiFID variables (risk, knowledge/experience, holding period, ESG preferences, client class), liquidity, PAC future flows over the holding period, and the model portfolio.
- **Scope split**: positions/transactions are **in scope** (advised products) or **out of scope** (e.g. non-advised products). Out-of-scope value is treated as liquidity in the analysis.
- **Suitability case selection** decides which checks run on which set:
  - all disinvestments
  - all investments out of scope
  - disinvestment + in-scope investment
  - disinvestment + out-of-scope investment
  - disinvestment + mixed
  - in-scope investments with or without earlier disinvestments from past proposals
  - default
- **Rules** are evaluated at proposal level, each returning pass, fail or not computable:
  - credit risk
  - market risk (VaR/volatility vs the profile limit)
  - concentration risk (issuer, currency, complexity)
  - complexity vs knowledge
  - target market
  - best-in-class
  - cost-benefit analysis (for switches)
  - minimum holding period vs the customer's horizon
  - valid MiFID variables
  - sustainability (ESG preferences), with sub-outcomes "incomplete ESG profile" and "PAI not applicable"
- **Outputs**:
  - overall outcome (OK/KO/not computable) and a sustainability outcome
  - failing product IDs per rule
  - risk before/after
  - asset-allocation comparisons: FAA L1/L2, CAA L2/L3, currency exposure, personal-assets division
  - analysis variables
- **History**: every verification is persisted with a progressive number, rule results, the profile snapshot and portfolio metrics (aum, liquidity, wealth, concentration). This is regulatory evidence.
- **Retry** on technical error. Re-verify before execution if the proposal is older than N days (file 04).

### REQ-PRO-007 Appropriateness check (MUST)
For transactions in non-advised products, or where suitability does not apply, check the appropriateness matrix: client class × knowledge profile × product complexity → allowed or not. Store the appropriateness evidence number. Products failing it are flagged on the transaction.

### REQ-PRO-008 Target market check (MUST)
Per transaction product, check the customer vs the product target market (file 01). A negative target market or mismatch is a warning or a block depending on the tenant policy, and it must be documented.

### REQ-PRO-009 Conflict of interest check (MUST)
Per transaction, call the conflict-of-interest service. Some operation types are inherently in conflict for online channels. Store CONFLICT / NOT_CONFLICT / UNCHECKED and disclose it in documents.

### REQ-PRO-010 Ex-ante costs and charges (MUST)
Per proposal, compute the costs of each product and transaction:
- entry/exit fees, ongoing charges, transaction costs, incidental costs, inducements
- totals in amount and %
- illustration of the cumulative effect of costs on return

The custodian commission/fee calculator gives per-order commissions and expenses. Advisor-entered commissions and expenses (per-mille and fixed amounts) are allowed within min/max bounds. Below the bank minimum → derogation (REQ-PRO-014). Feeds the ex-ante cost disclosure document (file 05).

### REQ-PRO-011 Enrichment step (MUST)
Collect the execution details per transaction:
- account holder / subscriber, and the debit/credit current account (IBAN)
- market/venue, and primary vs secondary market
- price limit / order type
- commissions and expenses
- PAC details: amount, frequency, duration, payment day
- income distribution choice
- address book entry ("rubrica")
- subscribers for joint accounts

An enrichment helper prefetches prices, venues, primary-market data and commissions in bulk. Real-time primary-market data are re-validated at save and at suitability; mismatches are flagged per product.

### REQ-PRO-012 Proposal CRUD and navigation (MUST)
- Create, get, modify, delete: a soft delete, allowed only in draft/definitive.
- Modify replaces the transaction set and recomputes enrichment.
- Step-transition validation.
- Actions on save: draft save, suitability verification, proposal save (definitive), confirm, sign, reject by client.
- Lists:
  - proposals per customer, filtered by status group and scope
  - proposals per advisor: paged, filtered by date range (default 30 days), status, branch and bank for managers, customer, type
  - Excel exports of both

Each list item carries the purchase CTV and sale CTV totals, and flags for "AML suspended", "third-party fund platform error" and "has legal guardian/support admin".

### REQ-PRO-013 Signature (MUST)
On "sign":
- **tenant check**: the tenant must be active
- **documents**: generate the signable documents (file 05): recommendation/suitability report, order forms, cancellation/withdrawal form, contract addenda, third-party platform subscription forms
- **e-signature**: send them to the e-signature/document-composition platform
- **guided proposals**: persist the guided product configuration
- **AML high-risk notification**: if any subscriber has a HIGH AML risk profile, email the AML officer of the tenant (address configured per bank) and mark transactions AML-suspended
- **hand-off to execution** (file 04)

Signable document lists can be retrieved per proposal.

### REQ-PRO-014 Supervision / four-eyes approval (MUST)
**Supervisor approval** is required when:
- the advisor is a supervised profile (tutoring: SUPERVISOR / SUPERVISIONED), or
- commissions/charges are derogated below threshold ("forced" derogation), or
- AML lock

Supervisors get a list of proposals pending approval and accept or reject each one. Status goes to CONFIRMED_PENDING_APPROVAL / SIGNED_PENDING_APPROVAL. If not approved in time → EXPIRED_NOT_APPROVED.

AML: an AML-suspended proposals list. Transactions can be AML locked/unlocked by an authorised user (file 04).

### REQ-PRO-015 Proposal report / simulation (MUST)
- PDF report of the proposal: portfolio before and after, transactions, suitability outcome, costs, and allocation comparisons.
- Simulation variant for SIMULATED mode.
- See file 05.

### REQ-PRO-016 Product information for a proposal (MUST)
For the products of a proposal, return the product information needed for disclosure: documents, costs and target market. When a delegate is operating, show the delegate's view.

### REQ-PRO-017 Customer portfolio after optimizer (SHOULD)
Return the full portfolio bundle (positions, allocations, liquidity) as it would look after the guided changes, for the FE preview.

### REQ-PRO-018 Advisor order-desk badges (SHOULD)
"New since last viewed" counters of proposals and orders per status macro-category, in time slots. The advisor's last-viewed timestamp is persisted. See file 07.

### REQ-PRO-019 Periodic suitability, ongoing assessment (MUST)
- A periodic job reads, per dossier, the portfolio-level adequacy results from the risk engine: market risk, concentration and sustainability contributors.
- It stores the outcome history (adequacy outcome + details) with periodic suitability dates.
- It exports results back to the core banking system.
- Failures raise alerts (file 07) and feed the annual suitability report.
- Regulatory driver: MiFID II Art. 25(6) periodic suitability.

### REQ-PRO-020 Multi-holder and delegation rules (MUST)
Supported account types: individual (PF), sole proprietorship (DI), joint (COI) and legal entity (PG).
- **Joint accounts**: all holders, or the selected subscribers, must sign. If the COI itself is a subscriber, all co-owners are subscribers.
- **Operators/delegates** with subscription power can act. Check "on behalf of whom" via the custodian.
- **Profile used**: for joint and legal-entity accounts, the MiFID profile used is the leader's if any, otherwise the most restrictive/maximum among related subjects. This is **[TENANT-POLICY]**.

## Data entities (conceptual)
- **Proposal** (header, see REQ-PRO-001), **ProposalStatusHistory/Action**, **ProposalReserve/LiquidityAllocation**, **ProposalGuidedProduct** (lock config, target weight).
- **ProposalTransaction**: see file 04 for its fields and lifecycle. Child records are enrichment, ext info, guided info, notes, counterparty, execution fields and actions.
- **ProposalSuitability** (proposal ↔ suitability run), **SuitabilityRun** (number, date, source, profile snapshot, outcome, metrics), **SuitabilityRuleResult** (rule, passed, metadata), **SuitabilityProduct / SuitabilityTransaction**, **SuitabilityRapport**.
- **AppropriatenessMatrix** (client class, knowledge, complexity → allowed), **AppropriatenessRequest** (evidence).
- **TargetMarketRequest** (evidence).
- **ProposalDocument** and **CustomerProposalDocumentSign**: documents and signature status.
- **OptimizerParameters** remembered per customer.

## Operations (functional)
- Check proposal creation. Create, get, modify and delete proposals.
- Optimize (guided). Rebalance (guided). Preview portfolio after optimization.
- Verify suitability / get suitability data. Get product costs. Get product information.
- List proposals (customer/advisor) and export them. List supervised proposals and approve or reject. List AML-suspended proposals.
- Get proposal report (PDF). Get signable documents. Get AML completion status per subscriber.
- Order-desk counters: get, and mark viewed.

## Async
- **Events** (suggested):
  - lifecycle: `proposal-created`, `proposal-definitive`, `proposal-confirmed`, `proposal-signed`, `proposal-expired`, `proposal-rejected`, `proposal-deleted`
  - checks and approvals: `suitability-verified` (with outcome), `proposal-approval-requested`, `proposal-approved`, `proposal-approval-rejected`
  - AML: `aml-high-risk-detected`
- **Jobs**: proposal validity expiry (daily), periodic suitability import (periodic), suitability outbound export (daily).
- **Consumers**: order service (on `proposal-signed`), reporting/document service, notification service.

## External integrations (generalized)
- **Portfolio optimizer & risk/suitability engine** [VENDOR]: analyze proposal, analyze total portfolio, generate proposal (optimizer), and rule evaluations. Design a provider port, because rules may later be computed in-house.
- **Custodian instrument service**: prices, primary market, commissions calculator, conflict-of-interest check.
- **Core banking**: positions, orders, current accounts, holders, delegates.
- **E-signature / document composition platform**: see file 05.
- **AML service**: customer AML risk profile (file 10).
- **Email**: AML officer notification.

## Regulatory drivers
- MiFID II: suitability (Art. 25(2)), appropriateness (25(3)), product governance / target market, conflicts of interest (Art. 23), costs and charges ex-ante (Art. 24(4)), record keeping (Art. 16(6)), suitability statement (Art. 25(6)), sustainability preferences (Reg. 2021/1253).
- IDD for IBIPs. AML Directive (AMLD).
- [IT] CONSOB rules on advisory and recordkeeping. Italian "advanced advisory" (consulenza evoluta/avanzata) service levels (file 09).

## Legacy-specific, do not port
- Provider-specific DTO shapes, the "GP Multilinea 18" special case and sub-position aggregation hacks. Generalize as "managed mandate with exposed sub-lines".
- Recovery code for missing holder/address-book fields.
- Group-specific in-house fund PAC multiple rule: keep it as a **[TENANT-POLICY]** rule.
- Mainframe order-entry "channel" codes.
