# 09 Advisory service levels, contracts and fees

## Purpose
This domain manages the **advisory contract** between bank and customer:
- the service level: basic, or "advanced" paid advice
- the advisory package
- the fee schedule (AUM-band rates, discounts, exceptions)
- fee computation and feeds to billing
- inducement/commission rebates tracking

## Requirements

### REQ-FEE-001 Advisory service type and package (MUST)
- **Investment service type**: ADVISORY vs NON-ADVISORY (execution/RTO). Determines whether suitability applies.
- **Advisory package**: name, advisory type (basic / advanced), advisory model, min and max fee in bps, and the product types included in the package.
- **Customer link**: each customer portfolio is linked to one package (file 02).
- **Model of fee computation**: WMP-computed vs computed by the external wealth/fee platform. **[TENANT-POLICY]**

### REQ-FEE-002 Advisory contract (MUST)
Customer contract fields:
- package, status (active/inactive)
- start date and expiry date (used for the information retention period)
- AUM band
- the date the fund-house fee arrangement started
- contract discounts (rate, validity)
- service level history: an upgrade from basic to advanced, or a downgrade, for example after a merger

Contract and policy delivery dates and the "paper waiver" (durable medium) flag sit on the customer record (file 10).

Alert "contract active – to renegotiate" (file 07).

### REQ-FEE-003 Fee schedule (MUST)
- **Base rate**, per tenant × AUM band × package × cooperative-member flag. **[TENANT-POLICY]**: "member" is a cooperative-bank concept; generalize to a "customer category" dimension.
- **Discount campaigns** per tenant with start/end dates, overriding the rate per AUM band × package × category.
- **Customer-specific contract discount** with validity.
- **Fee exceptions** per dossier: custom advisory fee rate, custom fund-house fee rate, stamp-duty exemption **[IT]**. Loaded by feed.
- **Rebates** per tenant: a percentage of fees rebated.

### REQ-FEE-004 Fee computation and billing feed (MUST)
- **Daily job**: computes and exports the fee data per dossier (package, rate after discounts and exceptions, AUM) to the billing/core banking system.
- **Contracts in scope**: active contracts and contracts that became inactive but are within the expiry/retention window. This handles the case where the service model was downgraded within the period.
- **Payments**: paid fees (payment date per dossier) are imported back by feed for reporting and ex-post costs.
- **Extraction**: "applied fees", with export.

### REQ-FEE-005 Commission rebates / inducements tracking (SHOULD)
- **Monthly import**: commission rebates received from product manufacturers, per ISIN and month.
- **Consultation**: list and filter (ISIN, month), export, with distinct ISIN and month lists.
- **Use**: feeds inducement disclosure (file 05) and the quality-enhancement justification.
- **Third-party commissions feed**: costs from the costs engine per (tenant, ISIN, source, type) plus an aggregated view (file 01).

### REQ-FEE-006 Customer contract AUM range (SHOULD)
- AUM band of the contract, recomputed from the daily AUM.
- Drives the rate band.

## Data entities (conceptual)
- **AdvisoryPackage** (name, type, model, min/max bps, included product types).
- **AdvisoryContract** (customer/dossier, package, status, start, expiry, AUM band, fund-house date), **ContractDiscount**, **ServiceLevelChange** (from, to, reason, date).
- **FeeSchedule** (tenant, AUM band, package, customer category, rate), **DiscountCampaign** (tenant, name, start, end) + **CampaignRate**, **FeeException** (tenant, dossier, fee rate, fund-house rate, stamp-duty exempt), **TenantRebate**.
- **FeeAccrual / DailyFeeRecord** (dossier, date, rate, AUM, amount), **FeePayment** (dossier, date, amount).
- **CommissionRebate** (tenant, ISIN, month, amount).

## Operations (functional)
- Get the customer's contract and package. Simulate the fee for a customer.
- Admin: manage packages, schedules, campaigns, exceptions.
- Extractions: applied fees, commission rebates (+ ISIN/month lists, export).

## Async
- **Jobs**: daily fees (outbound), paid fees (inbound), fee exceptions (inbound), commission rebates (monthly inbound), commissions/costs (inbound).
- **Events**: `advisory-contract-signed`, `advisory-service-level-changed`, `fee-computed`.

## External integrations (generalized)
- Billing / core-banking fee collection.
- Costs and inducements engine.
- Manufacturer rebate statements.

## Regulatory drivers
- MiFID II independent vs non-independent advice and inducements (Art. 24(7)–(9), Delegated Dir. 2017/593 Art. 11–12 on quality enhancement).
- Ex-post cost reporting.
- [IT] stamp duty on financial statements; CONSOB advisory regulations.

## Legacy-specific, do not port
- The cooperative "member/socio" flag as a pricing dimension. Generalize it to a customer category. **[TENANT-POLICY]**
- The group fund house's specific fee date field. Generalize to "manufacturer arrangement date".
