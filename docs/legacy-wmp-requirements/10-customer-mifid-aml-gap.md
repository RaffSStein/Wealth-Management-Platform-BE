# 10 Customer, MiFID profiling, IDD, AML: gap-analysis checklist

## Purpose
The new BE already implements customer onboarding, the MiFID questionnaire and risk profile (with renewal and versioning), the AML check, financials and goals. So this file is a **checklist of legacy capabilities** to diff against what exists. Items likely missing in a generic onboarding are marked ⚠.

## Customer model

### REQ-CUS-001 Account types (MUST) ⚠
Account types:
- **PF**: individual
- **DI**: sole proprietorship, with a unique legal representative
- **COI**: joint account; co-owners, joint or individual signature, and an optional "disjointed" variant
- **PG**: legal entity, with legal representatives and executors

Related subjects (roles):
- holder, co-owner, legal representative, delegate/operator
- legal guardian, support administrator
- beneficial owner (for AML)
- leader (the related subject whose profile is used for joint/legal-entity accounts)

Rules: for joint and legal-entity accounts, the MiFID profile is taken from the selected leader. Without a leader → alert UNSELECTED_LEADER. A fallback rule picks the most conservative/maximum profile. **[TENANT-POLICY]**

### REQ-CUS-002 Customer classification (MUST) ⚠
- **MiFID client class**: retail, professional (by right / on request), qualified counterparty, undefined.
- **Cluster/segment**: mass market, affluent, private.
- **Customer state**: PROSPECT → CONTRACT_SIGNED.
- **Acquisition mode**: standard, massive (bulk onboarding of an existing book), migration code.
- **Customer type**: core-banking customer vs WMP-only prospect.
- **Client type for products**: finance (securities), insurance (IDD) and member. **[TENANT-POLICY]**

### REQ-CUS-003 Customer master data (MUST)
- **Identity and company data**: personal data, company info (sector, ATECO activity code **[IT]**), profession sector and type, qualification (education), role, nationality, city.
- **Contacts and preferences**: contacts (email, phone, house phone); contact preferences by method, place, time slot and regularity.
- **Identity documents**: type, number, expiry, status valid/expired.
- **Dossier data**: opening date, acronym, category, contract/policy delivery dates, paper waiver, dossier under warranty (guarantee/pledge), withdrawal date.
- **Consents**: privacy and opt-in info (marketing consents) from the core banking system.

Sync with core banking:
- on-demand pull of the customer record from the core-banking customer registry, via the "customer data bus" call
- a daily customer-information feed
- the registry services cover personal data, addresses, documents, related subjects, domicile, activity and ATECO descriptions

**Lists for the advisor**:
- core-banking customers vs WMP customers
- search mode SIMPLE / ADVANCED, with include-field control
- PDF/XLS export
- a MiFID tab list showing questionnaire status per customer

Operations: create (prospect), modify, modify an external (core-banking) customer, delete prospect.

### REQ-CUS-004 Family and lifestyle profiling (SHOULD) ⚠
- **Family members**: type (spouse, child, …), personal data, interests and sports, CRUD in bulk or one by one.
- **Pets**: type and gender, as part of the household profile.
- **Customer interests**: art, food, wine, shows, wellness, shopping, volunteering, travel, sport. **Sports**: type, level, use.

These are commercial profiling sections of the "personal data questionnaire" with part types: customer hobby, family-member hobby, contact preference, mandatory data, sector data, role data, family-member data, financial position, incomes, needs.

### REQ-CUS-005 Financial situation (MUST)
- **Incomes**: net income, investment income, rent, other.
- **Expenditures**: by type and subcategory: recurring outings, one-off outings.
- **Personal assets**: financial at this bank, financial at other intermediaries, real estate, welfare/pension. Each has resources (property or product) split into the buckets INVESTMENT / RESERVE / LIQUIDITY.
- **Liquidity**: blocked liquidity, liquidity for recurring expenditure.
- **Reserves**: general reserve with allocations.

### REQ-CUS-006 Needs and goals (MUST)
- **Needs analysis**: needs of type INVESTMENT / RESERVE / LIQUIDITY, with need items and amounts. They feed the liquidity available to proposals (file 03).
- **Investment projects (goals)**: STANDARD or CUSTOM; category home, education, luxury, life goals, free time, other; amount, horizon, priority. Modified in bulk per customer.

### REQ-CUS-007 MiFID questionnaire (MUST)
- **Types**: MiFID individual, MiFID legal entity, a configurator, the personal data questionnaire and the needs analysis. Category LEGISLATIVE vs COMMERCIAL.
- **Structure**: parts, questions, answers. Question dependencies: an answer enables or disables follow-ups. Product-specific questions (knowledge per product family) with closed and open answers.
- **Scoring**:
  - **Risk profile**: weighted sum of answers → LOW, MEDIUM_LOW, MEDIUM_HIGH, HIGH (also INCOMPLETE, DECLINED).
  - **Knowledge/experience**: weighted. Some questions use the **max weight among answers** instead of the sum. Result LOW … VERY_HIGH. **Professional clients**: knowledge is not computed from sections A/B.
  - **Holding period**: from a specific question/answer mapping.
  - **ESG profile**: from answer weights, with detail values NO_PREFERENCE / NO_SPECIFIC_PREFERENCE. ESG questions E1–E4 carry the percentage preferences for taxonomy and SFDR, and PAI.
  - **Indicators**: further indicator values derived from the answers.
- **Status**: VALID / TO_COMPILE / EXPIRING / EXPIRED. Validity is set per tenant in months.
- **Renewal**: re-profile triggers notifications according to a filter: EVERYTHING, ONLY_ON_RISK_OR_KNOWLEDGE_CHANGE or ONLY_ON_NEW_QUESTIONS_CHANGE. A profile change can re-associate the model portfolio and raise a customer disclaimer.
- **Update mode**: STANDARD, or TECHNICAL_THRESHOLD_UPDATE, a technical update when the scoring thresholds change.
- **Data source**: WMP, core banking, or business simulation.
- **Answers storage**: compact "questionCode_answerCode" serialization (legacy detail). Correct answers can be retrieved for knowledge tests.
- **Signature**: answers are saved only after signature (file 05).
- **Integration**: sync with the core-banking MiFID registry. Get or save the questionnaire per subject, get indicators per subject/dossier, and get questionnaires to compile.
- **History**: history of questionnaires and answers is imported and kept.

### REQ-CUS-008 Customer ↔ product target market matrix (MUST)
- **Matrix**: client class × knowledge × risk × holding period × ESG → customer TM value.
- **Matching**: against the product TM (file 01).

### REQ-CUS-009 IDD questionnaire (SHOULD) ⚠
An insurance demands-and-needs questionnaire (IDD) for IBIP distribution.

### REQ-CUS-010 AML questionnaire and risk (MUST)
- **Questionnaire versions**: AML_V1 / V2, for individuals and legal entities.
- **Subjects**: individual, legal entity's beneficial owner, individual executor, legal-entity executor.
- **Configuration**: per-tenant questionnaire configuration.
- **AML service integration**: risk profile (irrelevant / low / medium / high) with detail and scoring per category (PEP, crime, terrorism, local lists, other), potential matches, ATECO/RAE/SAE codes **[IT]**, validity and expiry.
- **Proposal effects**: high-risk subscribers in proposals lead to AML officer email and order hold (file 03/04). AML completion status is tracked per proposal subscriber.
- **Notification channel**: an AML email endpoint is exposed to external channels (file 12).

### REQ-CUS-011 Third-party fund platform questionnaire (SHOULD) [VENDOR]
- A questionnaire for individuals and legal entities, needed to onboard on the external fund platform.
- Fields by section/subsection. A failure set is tracked.

### REQ-CUS-012 Privacy questionnaire (MUST)
- Privacy consents captured and signed (file 05).

### REQ-CUS-013 Massive onboarding (SHOULD) ⚠
- A batch onboards an existing customer book into the WMP, with staging, listener and cleanup.
- It assigns default models and packages.
- Tenant flag "massive onboarding".

### REQ-CUS-014 Reference data (MUST)
Lookups with i18n:
- genders, pet genders
- sport levels/types/uses, qualifications, profession types/sectors
- cities, nationalities, pet types
- family member types, income/expenditure types
- personal product/property types, identity document types

## Async
- **Feeds**: customer information (daily), MiFID data and answers history (daily), customer MiFID info outbound, MiFID profile change outbound, QMiFID by tenant outbound, visibility.
- **Events** (suggest checking that they exist): `customer-created`, `customer-updated`, `mifid-profile-completed`, `mifid-profile-changed` (risk/knowledge/ESG delta), `mifid-profile-expiring`, `mifid-profile-expired`, `aml-profile-updated`, `customer-leader-changed`.

## Regulatory drivers
- MiFID II client categorisation, suitability information (Art. 54–55 of Reg. 2017/565), sustainability preferences.
- AMLD (CDD/EDD, beneficial owner, PEP).
- IDD demands and needs.
- GDPR.
- [IT] fiscal code, ATECO/SAE/RAE codes, legal guardian / support administrator figures.

## Legacy-specific, do not port
- Mainframe customer-registry transaction names.
- Compact answer serialization strings.
- The "socio" (member) client type. Make it a tenant-defined customer category.
