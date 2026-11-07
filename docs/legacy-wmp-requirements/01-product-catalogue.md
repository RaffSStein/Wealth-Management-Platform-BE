# 01 Product catalogue and instruments

## Purpose
This is the master data for every investable instrument the advisor can propose. It covers:
- funds/SICAVs, ETFs, bonds, equities, certificates and warrants
- insurance-based investment products (IBIPs)
- discretionary managed portfolios (GP)
- pension funds and deposit certificates

It also exposes market, cost, risk, target-market, ESG and document data. Proposal, portfolio, suitability and reporting all depend on it.

## Requirements

### REQ-PRD-001 Instrument master (MUST)
Hold one instrument record per product, keyed by an internal product code. Natural identity is **ISIN + currency**; the same ISIN can trade in several currencies.
Fields:
- identification: ISIN, internal code, name
- issuer / management company, issuing country, issue currency
- product type and subtype
- instrument-type transcoding, i.e. a mapping to the risk-engine taxonomy
- issue date and maturity date, coupon flag
- complexity level
- risk indicator: the 1–7 SRRI/SRI from the KID
- min denomination, min tradable quantity, negotiable lot size
- "default" flag, source system
- best-in-class flag, SICAV/umbrella reference
- MiFID product category: a regulatory classification list of ~25 values, e.g. covered/senior/sovereign G7/non-G7 bonds, ETC/ETN, certificates by capital-protection band, harmonised/non-harmonised ETF, UCITS/AIF, listed derivatives, insurance class I/III/V/multi-line, G7/non-G7 equities
- deductible/allowance flag (franchigia) used in concentration rules

Product subtypes the logic branches on include:
- in-house managed portfolio vs third-party managed portfolio
- warrant, covered warrant
- domestic and foreign government bonds
- deposit certificates, option rights
- pension fund
- in-group bonds, convertible and non-convertible bonds

Rules:
- Instruments without a valid currency/ISIN pair cannot be proposed.
- Products "not administered" by the custodian cannot be moved in a proposal.

### REQ-PRD-002 Asset-class taxonomies (MUST)
Every product is classified on several parallel trees, used for breakdowns and model comparison:
- **Customer asset class (CAA)**: 3 levels, e.g. Equity → Global Equity → sub-class. This is the advisor-facing taxonomy.
- **Financial asset allocation (FAA)**: 2 levels, the risk-engine taxonomy, plus a currency-exposure breakdown.
- Look-through breakdowns from the market-data provider: equity region, equity sector, bond sector, bond maturity bucket, bond credit-rating bucket, fixed-income allocation, custom breakdowns.
- An asset-class proxy for instruments with no look-through data. A proxy fallback table maps product type to a representative proxy.

The taxonomies are reference data with i18n labels. Classification is loaded by feed (file 11).

### REQ-PRD-003 Product search for advisor (MUST)
- Paged search over the catalogue, scoped to what the user's tenant is allowed to distribute (see the product shelf, file 08).
- Free-text search plus filters: type, asset class levels, currency, issuer/company, risk class, ESG profile, market.
- **Advanced filter** mode returns progressive facets: the FE sends the first selections and the BE returns the reduced set of valid values for the remaining filters.
- A separate advanced-filter variant applies **inside a proposal**. It also excludes products the customer cannot buy, e.g. not in the shelf, not placeable, or failing target market.
- Lists of companies (issuers) for filter dropdowns.

### REQ-PRD-004 Product sheet (MUST)
Single-product detail for the product card:
- master data and classifications
- performance series (graph points) and risk indicators: volatility, VaR, Sharpe, max drawdown, performance over periods
- costs (REQ-PRD-008), PAC options (REQ-PRD-006), markets/venues (REQ-PRD-005)
- documents (REQ-PRD-009)
- target market (REQ-PRD-007), sustainability data (REQ-PRD-010)
- ratings for bonds

Field-inclusion control: the caller passes an `includeFields` list so heavy sections are loaded on demand.

**Secondary/virtual products**: a secondary product (a virtual class of a fund) shows only ISIN, name, currency and documents. Everything else comes from its primary product.

The product can be edited by authorised back-office users for enrichment fields.

### REQ-PRD-005 Markets and venues (MUST)
- Each product has 1..n trading venues (MIC / "piazza") with a default venue.
- **Primary market**: placements, auctions and new issues with a subscription window, available amount (plafond), min quantity and settlement/value date. These come from the custodian's real-time primary-market service. Products traded only on primary market are flagged, and their price comes from that service rather than the price feed.
- **Secondary market**: prices from the price service, using the default market price in the product currency.
- Exchange calendar is needed to compute value dates and "market closed" outcomes.
- FX rates table (daily feed) for converting to the reporting currency.

### REQ-PRD-006 Recurring investment plans, PAC (MUST)
Per product, the allowed PAC (recurring savings plan) options:
- plan code and description
- initial number of instalments, min/max number of instalments
- min/max instalment amount, min instalment increment
- min/max duration, whole-years-only flag, fixed-amounts flag
- "first payment must be a multiple of the instalment" flag
- frequency (monthly / quarterly / half-yearly) and allowed payment days

The rules drive validation of PAC subscriptions in proposals (file 03). PAC status for existing plans is ongoing / suspended / ended.

### REQ-PRD-007 Target market, product side (MUST)
- Per product, the MiFID II product-governance target market, expressed per customer dimension: client class (retail/professional), knowledge level, risk profile, holding-period profile, sustainability preference.
- Each value is either *positive* (P) or *negative* (N) target market.
- The customer-side matrix gives the allowed combinations (file 10).
- Used to decide whether a product can be offered to a customer, and in periodic target-market reports.
- A "target-market return code" taxonomy explains mismatch reasons.
- Loaded by feed. Can also be checked by external channels (file 12).

Regulatory driver: MiFID II product governance, Art. 16(3) and 24(2) of the Directive, and Delegated Directive 2017/593.

### REQ-PRD-008 Costs and charges, product level (MUST)
Per product, cost items classified by:
- type: one-off, ongoing/current, accessory, incidental/performance, transaction costs, incentives/inducements
- operation: purchase or sale
- aggregation level

Commission types include:
- entry/subscription fees and exit fees, fixed charges, additional-payment fees
- anti-dilution levy, distribution fee
- management fee / ongoing charges, transaction costs ex-ante
- mark-up, structured-product quotation costs
- max entry/exit costs and typical exit cost before the recommended holding period (RHP)
- incentives (inducements) retained by the distributor

Each commission has a source (which feed it came from) and a format (percentage, per-mille or fixed amount). A per-(tenant, ISIN) aggregated commission view exists for fast proposal costing.

Regulatory driver: MiFID II Art. 24(4), Delegated Reg. 2017/565 Art. 50, and PRIIPs KID.

### REQ-PRD-009 Product documents (MUST)
Per product, regulatory documents: UCITS KIID, PRIIPs KID, product fact sheet (retail and institutional). They are downloadable from the product card and proposal. Legacy fetches them from a document repository service.

### REQ-PRD-010 Sustainability / ESG data (MUST)
Per product:
- sustainability profile (S / non-S)
- SFDR classification (Art. 8 / Art. 9 category)
- taxonomy-aligned eco-sustainability category
- PAI (principal adverse impact) category
- sustainability risk category (low / medium-low / medium-high / high)

These are used to match the customer's ESG preferences (file 10) in suitability, and to generate "sustainability not verified / not suitable" alerts.

Regulatory driver: MiFID II sustainability preferences (Delegated Reg. 2021/1253) and SFDR.

### REQ-PRD-011 Minimum investable amounts (MUST)
- Per product and fund class: min initial subscription and min subsequent subscription.
- Plus "at least one unit" rules for some fund families.
- In-house funds with an active PAC require subsequent subscriptions to be a multiple of the PAC instalment. **[TENANT-POLICY]**

### REQ-PRD-012 Equivalent-product search for IBIPs (SHOULD)
Given an insurance-based product (IBIP) held or proposed, return the best "equivalent" non-insurance products per category, e.g. bond funds, government bonds, investment certificates and global equity.
- Ranking is by commission and cost gap vs the IBIP. A negative gap means the equivalent is cheaper.
- Prefer equivalents already in the customer's portfolio, then fill the top N.
- The IBIP must exist in the tenant's shelf.

Driver: Italian/EU IBIP distribution, IDD and the CONSOB comparison obligation **[IT]**.

### REQ-PRD-013 Fund category browser (SHOULD)
- Browse funds by category with dynamic filters. FE supplies all three asset-class levels, otherwise the request is rejected.
- Returns products per category for a catalogue view.

### REQ-PRD-014 Switchable funds (MUST)
- For a fund held, list funds it can be switched into without redemption/subscription: same management company/SICAV, and allowed by fund rules.
- A switch "causal" (reason code) table validates allowed switches.

### REQ-PRD-015 Product ratings (SHOULD)
- Credit ratings for bonds, from the rating feed and with an ordering scale.

### REQ-PRD-016 Fictitious instruments (COULD)
- Placeholder instruments used to represent liquidity or non-mapped holdings in risk analytics, by fictitious-instrument type.

### REQ-PRD-017 Retention of MiFID product data (COULD)
- Keep history of MiFID-relevant product attributes for evidence: product MiFID retention table.

## Data entities (conceptual)
- **Product**: internal code (PK), ISIN, currency, name, issuer, type/subtype, classifications, risk, complexity, flags. Relations: venues, costs, documents, PAC options, target market, ESG, ratings, minimums, breakdowns.
- **ProductType / Subtype** with ordering.
- **AssetClass trees**: CAA L1–L3, FAA L1–L2, and Product↔class with weights.
- **Breakdown** (product, breakdown type, bucket, weight).
- **Venue** (product, MIC, default flag). **ExchangeCalendar**. **FxRate** (date, ccy, rate).
- **CostItem / Commission** (tenant?, ISIN, type, source, format, value, operation).
- **PacOption** (product, rules above).
- **TargetMarketProduct** (product, dimension, value, P/N).
- **ProductDocument** (product, type, url/ref, language, version date).
- **ProductEsg**, **ProductRating**, **ProductMinimum**, **SwitchCausal**.

## Operations (functional)
- Search products: paged, filtered, faceted. A proposal-scoped variant exists.
- Get product sheet with selectable sections.
- Update product enrichment fields (back-office).
- Get PAC options, performance graph, venues and default venue for a product.
- List issuers/companies. Get advanced filter facets.
- Find IBIP-equivalent products. Find switchable funds.
- Browse fund categories, with facet values.
- Download product document.

## Async
- Daily or periodic feeds update master data, prices, analytics, costs, ratings, ESG, target market, minimums, breakdowns and FX (file 11).
- Suggested events: `product-created`, `product-updated`, `product-price-updated` (batched), `product-target-market-updated`, `product-costs-updated`, `product-suspended`.

## External integrations (generalized)
- **Market/fund data provider** [VENDOR]: fund universe, look-through breakdowns, performance, risk metrics, documents. Legacy uses a fund-data vendor via file exports.
- **Custodian instrument service**: instrument master, prices, venues, primary-market placements and auctions, repo/securities-lending availability, exchange calendar, cost and commission calculation, conflict-of-interest check per instrument.
- **Cost & charges engine** [VENDOR]: product cost aggregation and inducements.
- **Document repository**: KIID/KID/fact sheets.
- **Risk engine** [VENDOR]: instrument-type and asset-class transcoding tables are exported to it (outbound feeds).

## Regulatory drivers
MiFID II (product governance, costs and charges, sustainability preferences), PRIIPs, UCITS KIID, SFDR, and IDD for IBIPs.

## Legacy-specific, do not port
- "In-house group fund" and "in-group bond" subtypes are hard-coded. Model them as a tenant flag "issuer belongs to distributor group", which matters for conflict of interest. **[TENANT-POLICY]**
- Specific fund-family minimum rules hard-coded for the group's own asset manager.
- Fixed-width ISIN feeds from the group's custodian.
