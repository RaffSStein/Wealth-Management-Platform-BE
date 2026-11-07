# 08 Model portfolios and investment center (product shelf, buy lists)

## Purpose
The **investment center** is the central investment-office function of the group or tenant. It decides:
1. **Which products each tenant may distribute.** This is the *product shelf*, which legacy calls "multi-catalogue": a versioned list of products per product type with in/out changes.
2. **Which products are recommended.** These are *buy lists*: the default group list, a tenant-modified list and personalized lists.
3. **Which reference allocations apply to each customer segment.** These are *model portfolios* per profile × service level × cluster.

Advisors build guided proposals from these, and diagnosis compares portfolios against them.

## Requirements

### REQ-ICT-001 Model portfolio (MUST)
- **Header**: code, name, alias, description, author, creation date, release (version), invested amount, expected volatility and performance.
- **Composition**: products with target weights, at CAA L3 level and per product. A market choice (primary/secondary) is stored per product.
- **Statistics**: risk/return figures, and stats per component band (low/medium/high).
- **Segment applicability**, per tenant: the combination of risk profile × knowledge profile × holding period × ESG × advisory service type × client cluster. Data quality checks that every combination has at least one model.
- **Ownership**: models can be investment-center (group) models, tenant-custom models, or advisor-preferred models.
- **Upload pipeline**: models are uploaded by file with a data-quality step (file 11), a staging area and a final load.

### REQ-ICT-002 Model ↔ customer association (MUST)
- **Default association**: at onboarding, a batch job or a MiFID profile change assigns the best-fit model for the customer segment. Rules **[TENANT-POLICY]**:
  - match the segment (risk/knowledge/horizon/ESG/service/cluster)
  - pick the lowest-risk valid model if several match
  - joint/legal-entity accounts use the leader's knowledge profile, otherwise the maximum among related subjects
- **Advisor actions**: list selectable models for a customer, then change or force the association. Forcing requires the feature toggle.
- **Association modes**: DEFAULT, GUIDED_PROPOSAL, BATCH, DIAGNOSIS, DASHBOARD.
- **Customer model list**: per customer, the current model and the history of models.
- **Model risk options**: per customer, the risk levels the customer may choose a model from.

### REQ-ICT-003 Model portfolio suspension and reassignment (MUST)
A periodic job, in five steps:
1. Validate each active model's products against primary-market constraints: placeability and remaining plafond.
2. Detect dossiers associated with a model that is no longer valid (suspended for a tenant).
3. Deactivate those associations.
4. Re-run the association algorithm to assign new valid models, grouping dossiers by segment to minimise computations. If no valid model exists, assign an "empty model".
5. Create a customer disclaimer "new model associated" (file 07).

Data-quality also reports, per tenant, the model/segment pairs where the model has no products on the primary market or would leave a segment without a lower-risk model.

### REQ-ICT-004 Product shelf, "multi-catalogue" (MUST)
- **Status per version**: IN_PROGRESS → CURRENT_VERSION → LAST_VERSION → ARCHIVED.
- **Creation status**: PROSPECT → GENERATED → CONFIRMED.
- **Wizard steps**: upload documents, then build the multi-catalogue.
- **Rows by product family**: fund, ETF/ETC/ETN, stock, IBIP (insurance classes I/III/V/multi-line), investment certificate, in-group bonds, third-party bonds, deposit certificates, managed portfolios. Each row has a coupon type (accumulation/distribution).
- **Product focus/placed lists**: products placed by each tenant, product-to-tenant association, and a per-tenant configuration (which tenants adhere to the group investment center).
- **In/out tracking**: the products ENTERING/EXITING between versions. Listed and exported.

Operations:
- create a version (in progress), upload an Excel creation model, generate, confirm, download, delete in-progress
- list current and archived versions, and get a version with its rows
- get the Excel template for creation
- get products in/out

Rule: product **placeability** for a tenant = in the current shelf version for that tenant. Product search and proposal filter on it (file 01).

**Tenant adherence status**: GROUP / ADHERENT tenant / NOT ADHERENT tenant. Non-adherent tenants manage their own lists. **[TENANT-POLICY]**

### REQ-ICT-005 Buy lists (MUST)
- **Buy list**: name, status/owner (group, adherent tenant, non-adherent tenant), tenant, type (DEFAULT / MODIFIED / PERSONALIZED), parent list, creation and last-modified dates (group and tenant).
- **Buy list product**: product, flags PREFERRED / LOCKED. Modifications (ADD/REMOVE) are tracked as operations so the tenant's edits can be re-applied on top of a new group version.
- **Customer segments** a buy list applies to.
- **Model ↔ buy list association**: the optimizer uses the buy list as the investable universe (file 03 REQ-PRO-004).
- **Advisor preferences** on buy-list products.

Operations: list buy lists, list buy-list products, modify a buy-list product (add/remove/prefer/lock), get the products of a model portfolio.

### REQ-ICT-006 Investment center outbound feeds (SHOULD)
Export models, customer↔model associations, the shelf, the asset-class transcoding, the instrument-type transcoding, the managed-portfolio transcoding, the IBIP transcoding and the unit composition to the risk engine and the core banking system.

### REQ-ICT-007 Saa/Op rules, strategic asset allocation (COULD)
- Strategic and operational asset-allocation rules per segment (SAA/OP) and per product.
- Custom rules feed the optimizer as constraints.

## Data entities (conceptual)
- **ModelPortfolio** (code, release, header fields, owner tenant/group, status), **ModelPortfolioComponent** (model, product or asset class, weight, market choice), **ModelPortfolioStats**, **ModelSegmentApplicability**, **ModelSuspension** (model, tenant, reason, date).
- **CustomerModelAssociation** (customer portfolio, model, mode, date, active).
- **ShelfVersion** (status, creation status, validity date, creator), **ShelfRow** (version, product family, product, coupon type, tenant association), **ShelfTenantConfig**, **ShelfProductPlaced**.
- **BuyList** (name, type, owner, tenant, parent, status), **BuyListProduct** (list, product, flags), **BuyListModification** (list, product, op), **BuyListSegment**, **ModelBuyListAssociation**.
- **SaaOpRule**.

## Operations (functional)
- Models: list for advisor (filter: all / investment-center / custom / preferred), get model info and compare with real, modify a model (investment-center user), filter values, model risk options per customer, change/force association, feature-flag query.
- Shelf: the CRUD/wizard listed under REQ-ICT-004.
- Buy lists: list, products, modify.

## Async
- **Jobs**: model upload (DQ + final load), model suspension and reassignment (periodic), shelf load (DQ + final), buy-list feed, outbound exports.
- **Events**: `model-portfolio-published`, `model-portfolio-suspended`, `model-portfolio-associated`, `shelf-version-published`, `buy-list-updated`.

## External integrations (generalized)
- Risk engine / optimizer: consumes models and buy lists.
- Primary-market service: placeability and plafond for validation.

## Regulatory drivers
- MiFID II product governance: the distributor's target market and its product range.
- Suitability: models are the basis for advice.
- [IT] the "advanced advisory" service model.

## Legacy-specific, do not port
- Row types naming specific group bond issuers. Use a generic "in-group issuer bonds" with a tenant-group flag.
- Investment-center adherence encoded as a bank-group enum. Model it as a tenant relationship to a "central investment office" tenant.
