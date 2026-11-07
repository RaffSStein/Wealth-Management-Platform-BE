# 05 Reporting, costs and charges, documents and e-signature

## Purpose
This domain produces every customer-facing and regulator-facing artefact:
- PDF reports (diagnosis, monitoring, proposal, position keeping, customer list)
- ex-ante and ex-post costs and charges, inducements
- the signable document pack of a proposal or questionnaire, and its round-trip with an e-signature platform
- data extractions and Excel exports for compliance, network managers and back office
- target-market periodic reporting

## Requirements

### REQ-REP-001 Branded PDF reports (MUST)
Report types:
- **Portfolio diagnosis**: real vs model, adequacy variables, allocation, risk, alerts.
- **Portfolio monitoring**: performance (MWRR), CTV history, allocation, risk indicators, positions.
- **Proposal**: before/after portfolio, transactions, suitability outcome and rule details, ex-ante costs, allocation comparisons, product info.
- **Proposal simulation**: same layout, watermarked as a simulation, not a recommendation.
- **Position keeping**: positions statement.
- **Customers report**: an advisor's customer list.

Per-tenant **custom document parts**: "about us" logo, image and text per report type. Uploaded and managed by tenant admins, previewable. **[TENANT-POLICY]**: branding is per tenant.

Generation: server-side, from the same DTOs the FE shows. Returned as a download, and optionally archived.

Utility: a PDF utility endpoint that merges or stamps PDFs uploaded by the FE, e.g. appending FE-rendered charts, and uploads the result to the document archive.

### REQ-REP-002 Ex-ante costs and charges disclosure (MUST)
Built per proposal from product cost items and order commissions (file 01, file 03 REQ-PRO-010):
- **cost categories**: one-off, ongoing, transaction, incidental, ancillary-service costs, third-party payments (inducements)
- **breakdown**: shown as % and amount, split between product costs and service costs
- **effect on return**: illustration of the cumulative effect over the holding period, year 1 and at exit
- **delivery**: part of the proposal pack, before signature

Driver: MiFID II Art. 24(4), Reg. 2017/565 Art. 50 and Annex II.

### REQ-REP-003 Ex-post costs and charges, annual statement (MUST)
Annual per-customer statement of the costs actually incurred, with inducements received by the distributor:
- **calculation**: done by a costs engine from movements, positions and product costs. Legacy calls the custodian's costs/charges and inducement elaboration plus query services, and gets a document stream and the return net of costs.
- **inputs from the WMP**: the advisory fees paid (file 09), the extra-WMP operations and the execution details (file 04).

Driver: MiFID II Art. 50(9) of Reg. 2017/565.

### REQ-REP-004 Signable document pack (MUST)
Document types assembled for signature:
| Type | When |
|---|---|
| AML questionnaire | onboarding / renewal (file 10) |
| Privacy consent | onboarding |
| MiFID questionnaire | onboarding / renewal |
| Contract addendum / contract annex | when the advisory service level or contract changes |
| Single order module | per proposal: the list of all orders |
| Orders cancellation module | revoking orders |
| Withdrawal form | right of withdrawal for off-premises sales **[IT]** |
| Third-party fund platform subscription form | if the proposal includes funds of that platform |
| In-house fund subscription form | initial subscription or PAC activation on group funds **[TENANT-POLICY]** |

Signer determination rules:
- **default**: the operator signs. For subscription forms, the subscribers sign too.
- **joint account (COI)**: if the COI is the operator or a subscriber, all co-owners sign. Each co-owner is treated as an individual.
- **legal entity (PG)**: legal representatives sign, with a company prefix on the signature field.
- **delegates, legal guardians, support administrators**: excluded as signers for initial fund subscriptions. The holder signs instead.
- **cancellation module**: the operator signs, plus the subscribers if any order being cancelled is an initial fund subscription.

Field rules:
- **unchanged fields**: fields equal to the framework-contract defaults (amount, settlement account, venue) are suppressed.
- **switches**: in the single order module, switches are shown as redemption + initial subscription.
- **pre-filled forms**: the subscription form is included only if a fund was subscribed or a PAC activated.

Delivery medium flag: paper vs durable medium (non-paper).

### REQ-REP-005 E-signature round-trip (MUST)
- **outbound**: send the pack to the document-composition / e-signature platform ("print and sign"), keyed by type
- **inbound**: receive the outcome (signed, refused, error); for questionnaires, persist the answers only after signature
- **status**: track signature status per document and per signer
- **retrieval**: fetch signed documents from the document repository
- **notification**: email templates notify pending signature and AML events (templates per language)

### REQ-REP-006 Document archive (MUST)
- Upload documents: customer documents, proposal documents, PDF reports.
- Store metadata: type, customer, proposal, date, status valid/expired.
- Retrieve by customer or proposal.
- Identity documents of customers are stored with type and expiry (file 10).

### REQ-REP-007 Data extractions (MUST)
Back-office and compliance extractions, paged and exportable:
- **MiFID questionnaires**: status per customer (valid / to compile / expiring / expired) with profile fields; supports light and full modes.
- **Target market**: product vs customer TM checks.
- **Suitability**: suitability verifications performed.
- **Onboarded customers**.
- **Advanced advisory**: customers under paid advisory (file 09).
- **Applied fees**: advisory fees applied (file 09).
- **Commission rebates**: per ISIN and month; paged, with distinct ISIN and month lists for filters, and Excel export.

### REQ-REP-008 Target-market periodic report (MUST)
A scheduled job produces, per tenant, the periodic product-governance report: sales vs target market, including negative-TM sales. Legacy has a feeder step plus a report step. Placement dates exclude orders, and records with certain reasons are excluded from the stats variant. The report is exported to the manufacturer/compliance.

Driver: MiFID II product governance (distributor feedback to the manufacturer).

### REQ-REP-009 Advisor list exports (MUST)
Excel/PDF exports of filtered lists: customers, proposals, transactions, alerts, movements, investment center lists. Column ordering is configured per export.

### REQ-REP-010 Suitability evidence for regulators (MUST)
Every suitability, appropriateness and target-market verification is persisted with a progressive number, the inputs (profile snapshot), the rule results and the outcome. It must be retrievable for audits. See file 03.

## Data entities (conceptual)
- **Report** (type, tenant, customer?, proposal?, generated at, storage ref).
- **CustomDocumentPart** (tenant, report type, part = logo/image/text, content).
- **Document** (id, type, owner customer, proposal?, status, storage ref, signature status). **DocumentSigner** (document, subject, role, signed at, outcome).
- **CostDisclosure** (proposal, line items, totals, effect-on-return series).
- **ExPostCostStatement** (customer, year, items, inducements, doc ref).
- **TargetMarketReport** (tenant, period, rows).
- **ExtractionRun** (type, filters, requested by, file).

## Operations (functional)
- Generate report PDF (type, customer, proposal?). Manage custom document parts (list / upload).
- PDF utilities: merge/stamp, then archive.
- Upload document / list documents / download.
- Build and send the signature pack; get signable documents; receive the signature callback.
- Extractions (MiFID, TM, suitability, onboarded, advanced advisory, fees, rebates) plus exports.

## Async
- **Consumes**: `proposal-confirmed` (build pack), `proposal-signed`, `questionnaire-completed`.
- **Emits**: `document-generated`, `signature-requested`, `document-signed`, `document-signature-refused`.
- **Jobs**: target-market report (periodic), ex-post cost statement trigger (annual), daily fees output (file 09), suitability outbound.

## External integrations (generalized)
- **Document composition + e-signature platform** [VENDOR].
- **Document repository / DMS**.
- **Costs and charges engine and inducement engine** [VENDOR]: ex-ante and ex-post.
- **Email service**.

## Regulatory drivers
- MiFID II costs and charges ex-ante/ex-post.
- Suitability report (Art. 25(6)).
- Product-governance distributor reporting.
- Record keeping.
- PRIIPs/KID delivery. IDD for IBIPs.
- [IT] off-premises withdrawal right (TUF art. 30); CONSOB recordkeeping.

## Legacy-specific, do not port
- Hard-coded form field codes ("VAL5/VAL6/VAL7") of the composition platform. Keep them only in the adapter's template mapping.
- In-house fund family special forms: a **[TENANT-POLICY]** "manufacturer-specific subscription form" plugin.
- Italian fiscal-code-type flags inside forms **[IT]**.
