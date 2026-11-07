# 07 Alerts, notifications and disclaimers

## Purpose
This domain surfaces to advisors (and indirectly to customers) the conditions that need action:
- regulatory alerts: MiFID profile expiring or expired, portfolio no longer suitable, sustainability mismatch
- commercial alerts: model variance, maturing products, contract renegotiation
- notifications: news and communications
- disclaimers: one-off notices about changes made to a customer's setup by the system or by bank mergers

Alerts are mostly computed in batch from analytics feeds and customer data.

## Requirements

### REQ-ALR-001 Alert catalogue (MUST)
Each alert definition has a code, a type (LEGISLATIVE / COMMERCIAL / PERSONALIZED), a source (WMP-computed or risk-engine-computed), a blocking flag, localized texts and a call-to-action.

| Code | Meaning | Typical source |
|---|---|---|
| COMPLEXITY_CONCENTRATION | too much exposure to complex products vs knowledge | risk engine |
| ISSUER_CONCENTRATION | single-issuer concentration over limit | risk engine |
| CURRENCY_CONCENTRATION | non-base-currency concentration | risk engine |
| CREDIT_RISK | credit risk over profile | risk engine |
| MARKET_RISK | market risk (VaR) over profile | risk engine |
| MIFID_EXPIRED / MIFID_EXPIRING / MIFID_NOT_COMPILED | questionnaire status | WMP |
| PRODUCT_MATURITY | product near maturity | WMP |
| SUSTAINABILITY_NOT_VERIFIED / _NOT_SUITABLE | ESG preferences vs portfolio | risk engine |
| ECO_SUSTAINABILITY_NOT_VERIFIED / _NOT_SUITABLE | taxonomy-alignment preference | risk engine |
| PRINCIPAL_ADV_IMPACT | PAI preference not met | risk engine |
| HIGH_MODEL_VARIANCE / AVG_MODEL_VARIANCE | real portfolio far from model | WMP |
| Dossier anomalies: non-compliant joint dossier, non-compliant legal-entity dossier, online-trading dossier, consolidated online dossier, anomalous dossier, managed-portfolio dossier, dummy dossier | data-quality of dossier setup | WMP |
| UNSELECTED_LEADER | joint/legal-entity account without a designated leader for profiling | WMP |
| CONTRACT_ACTIVE_TO_RENEGOTIATE | advisory contract to renegotiate | WMP |
| FROM_ADVANCED_TO_BASIC_MERGER / _ACQUISITION | downgrade of service level after bank merger/acquisition | WMP **[TENANT-POLICY]** |
| DOSSIER_RS_BANKRUPTCY_CURATOR | dossier under bankruptcy curator, operations restricted | WMP **[IT]** |

Alert level: NO_ALERT / WARNING / CRITICAL.
Call-to-action: open diagnosis, compile MiFID questionnaire, renegotiate contract, acknowledge downgrade.
Keywords group codes for filtering.

### REQ-ALR-002 Alert generation (MUST)
- **Risk-engine alerts**: a daily feed of per-account rule results. Match each (bank, dossier) pair to a customer. Upsert active alerts and close resolved ones.
- **WMP alerts**: computed in batch (customer information job, MiFID status, maturities, model variance, dossier category checks).
- **Alert status per account**: tracked per day (last analytics), for change detection.
- **Blocking alerts**: may block proposal creation, e.g. MiFID expired.

### REQ-ALR-003 Alert queries (MUST)
- **Advisor desk**: paged alerts over all visible customers, filtered by code, keyword, type, level, branch and customer. Variants: full list and summary.
- **Count by customer**: number of alerts per customer, for badges on the customer list.
- **Customer card**: alerts of one customer, full and summary.
- **Excel export**: for both the advisor desk and the customer card. Column order is fixed.

### REQ-ALR-004 One-time alerts (SHOULD)
- Alerts shown once to a user until acknowledged, e.g. "your dossiers have been refactored from dossier-level to customer-level".
- List and acknowledge (modify).

### REQ-ALR-005 Personalized alerts (COULD)
- Type PERSONALIZED exists, but legacy has no API for advisors to create custom alerts.
- Candidate new feature: advisor-defined reminders and thresholds.

### REQ-ALR-006 Notifications / news feed (SHOULD)
- Notifications have a label, overview, date and items (rich content), plus a read status per user.
- **Operations**: list the latest, mark read/update, count unread.
- **Source**: published by back office.

### REQ-ALR-007 Customer disclaimers (MUST)
One-off notices shown on the customer card until deactivated. Types:
- MiFID profile updated: the new profile changed the suitable model
- new model portfolio associated automatically (file 08 suspension job)
- customer moved due to bank merger
- customer moved due to bank acquisition

Listing them for a customer also deactivates them (read-once semantics). The legacy endpoint does both in one call.

### REQ-ALR-008 Advisor disclaimers (SHOULD)
- Merger/acquisition notices addressed to the advisor.
- Bulk-deactivated by a job after the event.

### REQ-ALR-009 Order-desk badges (SHOULD)
- New proposals and orders since the advisor's last view, grouped by status macro-category and time slot.
- Stores the last-viewed timestamp.
- See file 03 REQ-PRO-018.

### REQ-ALR-010 Email notifications (SHOULD)
Templated emails:
- AML high-risk subscriber in a signed proposal → the tenant's AML officer
- pending execution issues → advisor or back office

Templates are per language. Recipients are configured per tenant.

## Data entities (conceptual)
- **AlertDefinition** (code, type, source, blocking, call-to-action, keyword, i18n texts).
- **CustomerAlert** (definition, tenant, customer, dossier, level, details/metadata json, first seen, last seen, status active/closed).
- **AccountAlertStatus** (account, date, snapshot).
- **OneTimeAlert** (code, user, acknowledged).
- **Notification** (label, overview, date, items), **NotificationRead** (user, notification).
- **CustomerDisclaimer** (customer, type, payload, active), **AdvisorDisclaimer** (advisor, type, payload, active).
- **OrderDeskLastViewed** (user, timestamp).

## Operations (functional)
- Alerts: list for advisor (full or summary), count by customer, list for customer, export, one-time list and acknowledge.
- Notifications: latest, mark read, count.
- Customer disclaimers: list-and-deactivate. Advisor disclaimers: list.
- Order desk: counters, mark viewed.

## Async
- **Consumes**: `account-analytics-received`, `mifid-profile-expiring`/`expired` (scheduled), `model-portfolio-associated`, `tenant-merger-completed`, `proposal-*`/`order-*` status events (badges), `aml-high-risk-detected` (email).
- **Emits**: `alert-raised`, `alert-closed`, `notification-published`.
- **Jobs**: alert import (daily), customer information/alerts recompute (daily), disclaimer deactivation after merger.

## External integrations (generalized)
- Risk analytics engine: rule/alert results per account.
- Email/SMTP service.

## Regulatory drivers
- MiFID II ongoing suitability and profile validity.
- Sustainability preferences.
- AML Directive.
- [IT] bankruptcy procedure restrictions.

## Legacy-specific, do not port
- Alert codes describing specific online-trading dossier categories of the group's core banking. Generalize as "dossier outside advisory perimeter / anomalous dossier" with tenant-configurable categories.
- The "refactor dossier to customer" one-time alert, which belongs to a past migration.
