# 06 Advisor, network hierarchy, roles and data visibility

## Purpose
This domain models who the users are and what they can see and do:
- advisors and managers in a bank network (tenant → branch → advisor → customer portfolio)
- their functional permissions
- their **data visibility scope** (own customers, branch, multi-branch, bank, multi-bank, all)
- delegation and supervision relationships

In legacy this is split between the group's IAM (SSO + "procedures/grants" profile service) and a visibility-cone service. It must become a clean, bank-agnostic module.

## Requirements

### REQ-ADV-001 Advisor registry (MUST)
- **Advisor**: code, tenant, branch, name, surname, link to the HR/customer id, role, tutoring type.
- **Roles**: private banker, private banker manager, commercial-area manager. Also standard advisor and bank manager as user profiles (REQ-ADV-003).
- **Tutoring type**: NONE / SUPERVISOR / SUPERVISIONED. Supervised advisors need approval of their proposals (file 03 REQ-PRO-014). A supervisor reference links an advisor to a supervisor.
- **Advisor ↔ branch**: an advisor can work for several branches (advisor-branch association).
- **Advisor ↔ dossier**: portfolio assignment (advisor dossier). Fed by a daily feed from the core banking system.
- **Reference**: an advisor reference table used to resolve managers.
- Lookup lists: advisors (filtered), all banks (tenants) and all branches of a bank, for manager filters.

### REQ-ADV-002 Tenant (bank) registry (MUST)
- **Bank fields**: code, type (bank, or another kind of legal entity in the group), status (ACTIVE / BUSINESS_SIMULATION, where simulation means demo or pre-production use with no real orders), MiFID questionnaire duration in months, acronym usage flag, massive-onboarding flag.
- **Bank information**: AML officer email, addresses, logo (bank logo table), other data. Fed by a feed.
- **Group-level super-banks**: whose users see all banks. **[TENANT-POLICY]**: model as "group admin tenant".

### REQ-ADV-003 User profiles (MUST)
Profiles:
- STANDARD_ADVISOR
- BANK_MANAGER
- PRIVATE_BANKER
- PRIVATE_BANKER_MANAGER
- COMMERCIAL_AREA_MANAGER
- SUPER_ABI: group-wide business user
- SUPER_ABI_TECHNICAL: group-wide technical/support user
- DASHBOARD_MANAGER: back-office uploads (file 11)

The current user's info is returned to the FE: profile, tenant, branch, advisor code, functions, visibility, and the selected visibility cone.

### REQ-ADV-004 Functional permissions (MUST)
Functions are a catalogue of (category, detail, action, type):
- **category**: customer, proposal, portfolio, MiFID, investment project, document signature, document repository, free proposal, guided proposal, settings, commissions/fees, management
- **detail**: free proposal, guided proposal, report, proposal status
- **action**: create, update, force, delete, invoke, download, extract, generic, force-update
- **type**: STANDARD (dispositive) or SIMULATION

Profiles map to functions via "procedures": a user procedure can grant function permissions, a visibility cone, or both.

**Special grants** on top of the profile:
- AML: AML supervisor, can unlock AML-locked orders
- FIDUCIARY: trust company operations
- DASHBOARD: back-office upload dashboard

Every API is protected by function + action. Simulated functions only allow simulated operativity.

### REQ-ADV-005 Data visibility scope (MUST)
Visibility types: NONE, OWN (own portfolio of customers), CAB (own branch), MULTI_CAB (list of branches), ABI (whole bank), MULTI_ABI (list of banks), ALL.

Every customer, proposal, transaction, alert and extraction query MUST be filtered by the caller's visibility. Legacy applies a central "visible customer entity filter" to queries. Group-level users (SUPER_ABI) bypass bank filters, but they cannot call some per-bank core-banking services.

### REQ-ADV-006 Visibility cones, network-segmentation integration (SHOULD)
Some tenants delegate visibility to an external network-management system that defines **visibility cones**:
- **cone type**: WALLET (portfolio of customers) or BRANCH
- **cone status**: PREDEFINED / ACTIVE

Behaviour:
- **selection**: a user can have several available cones and selects one per session. The selected cone filters data; the default is the predefined one.
- **dossier lookup**: dossier lists can be resolved by cone.
- **no-cone message**: per tenant, a message type for users without a cone, BLOCK (deny access) or INFORMATIVE (warn). Configurable.
- **visibility modes** combining cones: branch-only, portfolio-only, both, or bank manager.

Generalize as a pluggable **"external visibility provider"** strategy per tenant. **[TENANT-POLICY]**

### REQ-ADV-007 Advisor preferences (SHOULD)
- Preferred products (advisor product preference).
- Preferred model portfolios (advisor portfolio-model preference).
- Buy-list product preferences (file 08).
- These are used as filters ("preferred") in product search and model selection.

### REQ-ADV-008 Advisor disclaimers (SHOULD)
Notices addressed to the advisor about portfolio changes caused by bank mergers/acquisitions (types: merger, acquisition). Listed for the advisor and deactivated once read. See file 07.

### REQ-ADV-009 Delegation / acting on behalf (MUST)
- An operator (delegate, legal guardian, support administrator) acts for a holder.
- "On whose behalf can X operate", checked via core banking.
- Delegate-subscription-power flag on proposals.
- Different signer rules apply (file 05).

### REQ-ADV-010 Technical credentials (SHOULD)
- Internal/external technical credentials for batch and system-to-system calls: batch user, platform integrations, and a "skip filter check" credential used by trusted internal callers.
- In the new BE this becomes service accounts via OAuth2 client credentials, with scopes. No credentials in config files.

### REQ-ADV-011 Authentication (MUST)
- Legacy uses the group's proprietary SSO, with server-side session and token refresh/poll/logout endpoints.
- New BE: standard OIDC/JWT via platform-core.
- The profile/procedure lookup becomes a claims-enrichment or permission service.

## Data entities (conceptual)
- **Tenant** (code, type, status, parent group, config), **Branch** (tenant, code, name, region).
- **Advisor** (code, tenant, home branch, role, tutoring, supervisor), **AdvisorBranch**, **AdvisorPortfolioAssignment** (advisor ↔ dossier/customer).
- **UserProfile** (user, profile type, tenant), **Function**, **Procedure** (profile → functions, visibility), **UserGrant** (AML / FIDUCIARY / DASHBOARD).
- **VisibilityScope** (user, type, list of branches/tenants), **VisibilityCone** (user, external id, type, status, selected).
- **AdvisorPreference** (products, models), **AdvisorDisclaimer**.

## Operations (functional)
- Get current user info (profile, functions, scope, selected cone).
- List advisors (filtered), list tenants, list branches per tenant.
- Visibility cones: list available, get selected, select. Tenant no-cone message type: get / set.
- Advisor disclaimers: list.

## Async
- **Feeds**: advisor registry and advisor↔dossier assignment (daily), bank information (daily), visibility (inbound daily from network management, outbound export).
- **Events**: `advisor-created`, `advisor-updated`, `advisor-portfolio-reassigned` (the customer changes advisor, which matters for alerts and proposals), `visibility-scope-changed`.

## External integrations (generalized)
- **Identity provider** (OIDC).
- **Permission/profile service** of the bank group.
- **Network-segmentation / visibility-cone service** [VENDOR/TENANT].
- **HR / advisor master feed** from core banking.

## Regulatory drivers
- MiFID II staff knowledge and competence (ESMA guidelines). The advisor qualification may gate functions.
- Segregation of duties (four-eyes for supervision, AML).
- GDPR need-to-know (visibility).

## Legacy-specific, do not port
- The custom SSO servlet with session token juggling.
- Hard-coded "super bank code" and "branch code ≥ 700 means group branch" rules: these become tenant configuration.
- Bank-type enum naming specific group entities. Use a generic `tenantType`.
