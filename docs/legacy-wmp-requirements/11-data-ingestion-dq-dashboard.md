# 11 Data ingestion, data quality and back-office upload dashboard

## Purpose
Most of the WMP's reference and analytic data arrives as periodic file feeds (~80 batch jobs in legacy), from:
- the core banking system
- the risk/analytics vendor
- the fund-data vendor
- the costs engine
- the investment office

A data-quality (DQ) pipeline validates the files before they reach the final tables. Back-office users can upload some files themselves through a dashboard.

The new BE should turn this into ingestion services: file or stream → validate → stage → publish events.

## Requirements

### REQ-ING-001 Generic file ingestion pipeline (MUST)
Every feed follows the same pattern:
1. Detect the file in the inbound location, with a naming convention per feed and tenant.
2. **Syntactic validation**: format, mandatory fields, types, duplicates.
3. **Semantic validation**, only if step 2 passes:
   - referential checks: the ISIN exists, the product exists on the right currency (with a fallback to ISIN-only)
   - business checks: weights sum to 100%, every segment has a model, products are on the primary market, and so on
4. Write evidence to DQ tables: file load history, per-row result (OK / WARNING / ERROR) with reason codes, and alerts.
5. If valid, replace or merge the final data. Old data is deleted only when the new data is valid.
6. Track the file lifecycle status (e.g. RECEIVED → VALIDATED → WRITTEN).
7. **Periodic check**: if no new file arrived, verify that the last load is still valid or fresh, and alert if not.

Skip logic per record, with a skipped-records log file.

### REQ-ING-002 Back-office upload dashboard (SHOULD)
Users with the DASHBOARD grant can upload files of these types:
- best-in-class list
- strategic/operational asset allocation (SAA/OP)
- product shelf (multi-catalogue)
- model portfolio load
- fund additional info
- commissions
- asset-class mapping (AAC)
- managed-portfolio asset-class mapping

For each upload:
- **configuration per file type**: max MB, allowed extensions, max parallel jobs
- **validate-only mode**: feedback without writing
- **job status**: running jobs and available slots
- **history**: last upload status per file type with validation evidence, plus the full upload history

### REQ-ING-003 Feed catalogue (MUST, port as needed)
Inbound feeds, functionally:

| Domain | Feed (functional) | Frequency |
|---|---|---|
| Product | Security master / instrument registry; product–venue registry; initial product/issuer/MIC loads | daily |
| Product | Fund universe and fund data; fund additional info (DQ); SICAV registry | daily |
| Product | Product MiFID categories; complexity | daily |
| Product | Product minimums and minimum investable amounts | daily |
| Product | Product ratings | daily |
| Product | ESG data | daily |
| Product | Target market per product | daily |
| Product | Security analytics (risk metrics per instrument) | daily |
| Product | Equity sector / region look-through (full + delta); fixed-income allocation; custom breakdowns (DQ) | daily/weekly |
| Product | Asset-class proxy (DQ), managed-portfolio asset class (DQ) | on change |
| Product | Best-in-class list (DQ) | on change |
| Product | Commissions/costs from the costs engine; in-house fund commissions | daily |
| Product | FX rates | daily |
| Product | Pension broker composition | periodic |
| Product | Operation types and priorities | on change |
| Portfolio | Customer AUM | daily |
| Portfolio | Current accounts | daily |
| Portfolio | Deposit certificates | daily |
| Portfolio | Managed-portfolio position keeping | daily |
| Portfolio | Account analytics (risk engine) | daily |
| Portfolio | Monthly analytics | monthly |
| Customer | Customer information | daily |
| Customer | Dossier information | daily |
| Customer | MiFID questionnaires and answers history | daily |
| Customer | Visibility (network segmentation) | daily |
| Advisor | Advisors and advisor↔dossier | daily |
| Advisor | Bank information | daily |
| Advisory | Periodic suitability results and contributors | periodic |
| Advisory | Alerts (risk engine) | daily |
| Investment office | Model portfolios (DQ + final), model suspension | on change / periodic |
| Investment office | Shelf / multi-catalogue (DQ + final) | on change |
| Investment office | Buy lists | on change |
| Orders | Extra-WMP performed operations | daily |
| Orders | Transaction execution details | daily |
| Fees | Paid fees, fee exceptions, commission rebates | daily/monthly |
| Other | Contributors/workflow extraction | periodic |
| Other | Audit log | daily |
| Other | Market-data vendor raw files (download, dump, cleanup) | daily |

Outbound feeds:
- **To the risk engine**: security list, security breakdown, asset-class transcoding, instrument-type transcoding, managed-portfolio transcoding, IBIP transcoding, unit composition, model portfolios, customer↔model associations, shelf, in-group issuer list, advisory category, customer wealth data.
- **To core banking / compliance**: suitability results, periodic suitability, customer MiFID info, MiFID profile changes, MiFID questionnaire by tenant, daily fees, fund data, the target-market report, the audit log and visibility.

Dataprocess (internal) jobs:
- onboarding (massive)
- product staging cleanup
- customer MiFID info, MiFID questionnaire profile
- the target-market report feeder
- customer disclaimers for mergers/acquisitions, and their deactivation

### REQ-ING-004 Tenant merger/acquisition batch (SHOULD)
Pre-steps, MiFID + AUM moves, ex-post metric moves, the target-market report move, a history table and an external transaction migration history. See 00-index REQ-XC-006.

### REQ-ING-005 Scheduling and locking (MUST)
- Every job has a per-environment cron expression and a timezone.
- Distributed locking: a single runner per job.
- Jobs can also be triggered on demand (back office).
- A job-completion listener records the outcome.

### REQ-ING-006 Data purge per tenant (SHOULD)
- Delete all data of a tenant older than a date (see 00-index REQ-XC-007).

## Data entities (conceptual)
- **FeedDefinition** (code, source, tenant scope, schedule, file pattern, validators).
- **FileLoad** (feed, tenant, file name, received at, lifecycle status, result).
- **FileLoadRowResult** (load, row, result, reason, field).
- **DqAlert** (load, type, detail).
- **Staging tables** per feed (or a generic staging JSON).

## Async
- **New BE approach**: ingestion service(s) publish domain events after the final write, e.g. `instrument-master-updated`, `fx-rates-updated`, `account-analytics-received`, `model-portfolios-loaded`, `shelf-loaded`, `feed-load-failed`.
- **Prefer streams/APIs** where providers support them. Keep file adapters for the rest.

## External integrations (generalized)
- SFTP/file share for vendor and core-banking files.
- Market-data vendor download API (login, data output, save raw file).

## Regulatory drivers
- Data quality underpins suitability and product governance.
- Record keeping for evidence of the data used.

## Legacy-specific, do not port
- Fixed-width record layouts, mainframe record codes and file names.
- Vendor-specific delta/temporary tables. Implement them inside adapters if needed.
