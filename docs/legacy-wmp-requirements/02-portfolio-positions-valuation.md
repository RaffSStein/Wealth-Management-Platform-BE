# 02 Portfolio, positions and valuation

## Purpose
This domain builds the customer's investable wealth view: positions, liquidity and valuation, aggregated across their custody accounts. It serves three uses:
- monitoring: performance, risk and asset allocation
- diagnosis: comparison against an assigned model portfolio
- the "AS-IS" starting point for proposals

The core banking system is the source of truth for holdings. The WMP builds derived and analytic views on top of it.

## Key concepts
- **Customer**: natural person, legal entity, joint-holder group or sole proprietorship. See account types in file 10.
- **Dossier**: a custody/securities account at the bank. A customer can have several, and a dossier can have several holders. The advisory relationship and the MiFID profile are anchored on the dossier plus its principal holder.
- **Relationships ("rapporti")**: dossier, deposit account, current account (liquidity) and cash deposit, all linked to a dossier.
- **Position identifier**: ISIN + currency + account holder + sub-account. Positions are also aggregated by ISIN + currency for analytics.
- **Managed portfolio (GP)**: a discretionary mandate held as a position. It may contain *sub-positions*, e.g. multi-line mandates whose underlying lines are exposed individually.
- **Position value types**: free, pledged (collateral), or "from previous proposal", i.e. committed by a pending proposal and not movable.

## Requirements

### REQ-PTF-001 Real-time position retrieval (MUST)
For a customer and dossier, fetch positions from the core-banking position service:
- quantity, market value (CTV), average cost / invested amount, price, price date, accrued interest, currency, FX
- pledge/lock status, PAC plan details

Also fetch current accounts linked to the dossier and their balances. A current account can be linked to several dossiers; de-duplicate it.

Fallbacks:
- If the customer is not onboarded in WMP, holders are fetched from the core-banking "holders" service.
- For a "joint/operator" request, resolve the effective principal owner of the dossier.

### REQ-PTF-002 Wealth aggregation (MUST)
Build the customer's total wealth:
- securities positions, plus managed-portfolio positions (in-house managed portfolios come from a daily position-keeping feed rather than real time), plus liquidity (current accounts and deposit certificates)
- also "co-owned with same holders" positions (pari-titled)

Two aggregation modes: STANDARD (advisory perimeter only) and ALL (everything).

Exclusions and overrides by dossier category:
- online-trading dossiers, consolidated online dossiers, fictitious dossiers, anomalous dossiers, managed-portfolio dossiers and administered dossiers
- online-trading/execution-only dossiers are outside the advisory scope **[TENANT-POLICY]**

### REQ-PTF-003 Potential portfolio, AS-IS plus in-flight orders (MUST)
Build a "potential" AS-IS portfolio by merging real positions with orders already sent but not yet settled, and with transactions of proposals already signed and in execution.
- Guard: at most one in-flight transaction per position identifier, otherwise raise an error.
- Positions moved by a previous proposal are flagged and cannot be moved again by a new proposal.

### REQ-PTF-004 Portfolio TO-BE simulation (MUST)
Given AS-IS and a list of transactions, compute the TO-BE portfolio:
- quantities and CTV per position, new positions, weights, liquidity after operations
- PAC future instalments over the holding period, counted in exposure
- managed portfolio parents and sub-positions realigned

Used by proposal (file 03).

### REQ-PTF-005 Liquidity model (MUST)
Liquidity sources:
- current-account balances
- projected liquidity: sells settling in the future
- surplus liquidity from the needs analysis (reserve / liquidity / investment buckets, file 10)
- "new liquidity" brought in by the customer
- liquidity moved to a current account

Value-date rules:
- funds: redemption proceeds at T+5
- secondary-market bonds: issue date if in the future, otherwise trade date
- primary market: the placement's value date
- others: T+2

These are **[TENANT-POLICY]**, defaulted by market convention.

### REQ-PTF-006 Portfolio summary (MUST)
For a customer portfolio:
- total CTV, invested amount, gain/loss, liquidity
- allocation by CAA level 1/2/3
- allocation by FAA level 1/2 and currency exposure
- look-through breakdowns: equity region and sector, bond sector, maturity, rating

The FE can request either an aggregated or a detailed product list. Views: MONITORING, DIAGNOSIS, POSITION_KEEPING.

### REQ-PTF-007 Performance (MUST)
- **Money-weighted return (MWRR)**: graph points and a summary table over periods: YTD, since inception, 1M, 3M, 6M, 1Y, 3Y, 5Y and custom range.
- **YTD performance** per portfolio and per customer.
- **CTV history graph** vs invested amount over time.
- Performance comes from the custodian's performance service, which computes MWRR on movements and positions.

### REQ-PTF-008 Risk indicators (MUST)
Portfolio-level volatility, VaR, Sharpe ratio, max drawdown and performance, from the risk-analytics engine plus fund-level metrics.
Analysis variables: average rating, duration, dividend yield, P/E.

### REQ-PTF-009 Ex-post account analytics (MUST)
A periodic (monthly/daily) feed from the risk engine supplies, per account:
- ex-post analytics: duration, P/E, dividend yield, adherence to model
- risk analytics, and knowledge/experience vs product complexity
- service level, time horizon, sustainability risk
- contributors to market risk, concentration and sustainability

These drive alerts (file 07) and periodic suitability (file 03).

### REQ-PTF-010 Movements / transactions history (MUST)
- Paged list of account movements: date, operation type, ISIN, quantity, amount, status.
- Domain-driven filters: the list of operation types and statuses comes from the custodian.
- Excel download.

### REQ-PTF-011 Position keeping API for other channels (MUST)
Expose to other bank channels (internet banking, CRM) for one or more customers:
- financial position summary
- portfolio details
- single position detail
- YTD performance

See file 12.

### REQ-PTF-012 Model portfolio association (MUST)
Each customer portfolio may be linked to a **model portfolio**, the reference allocation chosen from the customer's profile (file 08). The link stores:
- model, adequacy score, association mode (default / guided proposal / batch / diagnosis / dashboard)
- advisory package, risk profile, knowledge profile, client cluster (mass market / affluent / private)
- AUM and the advisor

Operations:
- get the model compared with the real portfolio, with gap per asset class and a gap threshold of low / medium / high
- change the associated model
- force a model, if the feature is enabled for the tenant
- list selectable models (filter: all / investment-center / custom / preferred)

### REQ-PTF-013 Portfolio diagnosis (MUST)
A diagnosis view (and PDF report, file 05) compares the real portfolio with the model portfolio and with the customer's MiFID profile:
- adequacy variables at portfolio level: complexity, credit risk, market risk, concentration, minimum holding period, best-in-class, cost-benefit, sustainability
- shows variance from the model, which can raise "high/average model variance" alerts

### REQ-PTF-014 Deposit certificates and pension positions (SHOULD)
- Positions in deposit certificates come from a daily feed.
- Pension fund positions come via the pension broker composition feed. Both are included in wealth.

### REQ-PTF-015 Customer AUM daily (SHOULD)
- Daily AUM snapshot per customer/dossier from a feed.
- Used for fee bands (file 09), segmentation and dashboards.

## Data entities (conceptual)
- **Dossier** (tenant, id, category, status open/closed, opening date, holders, principal, signature type individual/joint, advisor, branch).
- **Rapport** (dossier, type, iban/number, balance).
- **Position** (dossier, product, holder, quantity, ctv, avg cost, price, price date, ccy, lock type, value type). **PositionSubPosition** for managed portfolios.
- **CustomerPortfolio** (customer, model, association mode, adequacy, package, profiles, cluster, aum, advisor). **CustomerPortfolioAllocation** (CAA L1–L3 snapshot).
- **AccountAnalytics / MonthlyAnalytics / ExpostAnalytics / Contributors**.
- **CustomerAumDaily**, **ManagedPortfolioPositionKeeping**, **DepositCertificatePosition**.

## Operations (functional)
- Get customer wealth and portfolio summary, with view and aggregation options.
- Get portfolio product positions, aggregated or all.
- Get performance graph, performance table, YTD, CTV graph and risk indicators.
- Get monitoring data.
- Get movements with domain filters, plus Excel export.
- Model portfolio: compare, get info, change association, force, list selectable, filter values.

## Async
- Position-keeping feeds: daily managed-portfolio positions, current-account balances, deposit certificates, AUM and pension compositions.
- Analytics feeds: account analytics (daily) and monthly analytics.
- Suggested events: `portfolio-valuated`, `model-portfolio-associated`, `model-portfolio-association-changed`, `account-analytics-received`.
- Caching: positions are real-time but expensive, so cache per request/session.

## External integrations (generalized)
- **Core-banking position service**: positions, dossiers, holders, current accounts, PAC details, orders, movements and movement types, MiFID indicators per dossier.
- **Performance service**: MWRR, position-opening dates, average assets.
- **Risk analytics engine** [VENDOR]: portfolio/total-portfolio analysis, breakdowns, risk, rule evaluation.
- **Market-data provider**: look-through breakdowns.

## Regulatory drivers
- MiFID II ongoing suitability assessment: periodic report, Art. 54(12) of Reg. 2017/565.
- Ex-post costs (file 05). Wealth/AUM bands for fees.

## Legacy-specific, do not port
- Three different mainframe transaction calls merged with special rules to rebuild current accounts. Generalize as a "core-banking accounts adapter" returning a clean list.
- Special handling of the group's own managed-portfolio liquidity line, which is excluded from current accounts and re-added as a position.
- Monetary values delivered ×100 and divided back. This is an adapter concern.
