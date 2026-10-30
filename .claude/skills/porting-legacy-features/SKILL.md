---
name: porting-legacy-features
description: Ports a feature from the legacy WMP backend (Java 8 / Spring Boot 2.2, monolithic, single banking group) into the new bank-agnostic, event-driven WMP microservices, and hands the matching UI mocks to the FE session. Use when the user asks to port, migrate, re-implement or plan a legacy feature or REQ-<DOMAIN>-NNN requirement, or mentions the legacy WMP, legacy-wmp-requirements, or a gap against the legacy platform.
---

# Porting legacy features

Ports **behaviour**, never code. The legacy system is the functional reference. The target repo's rules
(`CLAUDE.md`) always win over legacy design. Every feature lands in the microservice that owns its domain.

## Inputs

- Requirements: `C:/Users/Raffaele/Desktop/WMP/legacy-wmp-requirements/` (`00-index.md` plus one file per domain).
  Read `00-index.md` and the domain file in scope. Load another domain file only to follow a REQ id that the
  in-scope requirements, or the domain map's "Spans" column, point to. Grep for that REQ id and read just its section.
- More depth (algorithms, state rules, field lists):
  1. If you can call `ListAgents` and it lists the legacy session (`java-risparmioeprevidenza-wmp-backend-*`), ask it
     by SendMessage: behaviour only, numbered questions.
  2. Otherwise (no `ListAgents` tool, e.g. you are a subagent, or the session isn't listed), read the legacy code,
     read-only: `C:/Users/Raffaele/Desktop/WMP/java-risparmioeprevidenza-wmp_backend/`. Grep for the feature's terms,
     then read only the classes that implement the rule. Navigation: inbound feeds are under `batch/**/inbound/<feed>/`.
  Either way, extract behaviour only. Never copy code, package or class names, bank names, credentials or config values.
  Go deeper before the spec only for the agreed scope, and only where a rule's algorithm or threshold shapes the
  contract or the tests. Everything else goes under Open questions.
- Target rules: `CLAUDE.md`, plus [reference/target-architecture.md](reference/target-architecture.md).

## Prerequisites (Phase 0)

| Prerequisite | Needed before |
|---|---|
| ✓ **Built.** Multi-tenancy in platform-core: new business entities extend `TenantScopedEntity` (see `core/platform-core/README.md`, "Multi-tenancy") | **every** domain port |
| Per-bank settings: typed values in `bank-service` tables (not files), feature flags in `profiler-service`, cached reader in platform-core | any port with `[TENANT-POLICY]` items |
| Transactional outbox in platform-core | ports whose events create, execute or change orders or holdings (proposal, order, the order-side suitability re-check). Derived snapshots fed by files (AUM, valuations) don't need it: a failed run is simply re-run. |
| Scheduled-job support in platform-core (single runner across instances; the lock library is a new dependency the user must approve) | any port with a scheduled job (feeds, expiry, end-of-day) |

While a prerequisite is missing, steps 1–4 still run: analyse and write the spec, designing as if the prerequisite
existed (e.g. `tenantId` fields), and list the blockers at the top of the spec. Steps 5+ wait until the prerequisite
is built, or the user explicitly says to proceed with a `//TODO:` marker.

## Workflow

Copy this checklist into your reply and keep it updated. Marks: `[x]` done, `[~]` partly done or waiting, `[ ]` not started.

```
Port progress: <REQ ids>
- [ ] 0. Prerequisites: Phase 0 status + missing upstream events/services
- [ ] 1. Scope: REQ ids, MUST/SHOULD/COULD, tags ([IT] [TENANT-POLICY] [VENDOR])
- [ ] 2. Place: owning service + events crossing boundaries (domain map)
- [ ] 3. Gap: what the target already has vs what is missing
- [ ] 4a. Port spec written (incl. UI mock plan)
- [ ] 4b. Port spec approved by the user
- [ ] 5. Contracts first: api-data / event-data YAML, kafka-properties
- [ ] 6. Implement + tests, build green
- [ ] 7. UI mocks drawn on the canvas + CHANGELOG entry
- [ ] 8. Handoff: FE session message, README, requirement traceability
```

**0. Prerequisites.** Check Phase 0 (above). Also list cross-service inputs the port needs that don't exist yet,
such as an event another service doesn't publish (e.g. `customer-mifid-profile-updated`) or a skeleton service that must
first be migrated to the 3-submodule split. Adding a missing event to the owning service's `*-event-data` is allowed as
part of the port, if the spec lists it.
- **Boundary of a port:** it builds its own service fully. In other services it adds only the event contract (and a
  skeleton split if needed). The other side's logic (e.g. order-service's reconciliation job that fires the re-check)
  is a follow-up port, listed in the spec, unless the user says to include it.
- **Inputs with no producer yet** (no feed, no upstream event): reference data may come from seed SQL in
  `src/main/resources/sql/` marked `//TODO:` with the follow-up port. A read model that depends on a missing
  upstream event is defined but stays empty until that event exists. Neither blocks steps 5+.
- **Platform-core changes** the port needs (e.g. moving `Pageable`) are listed in the spec. Approving the spec
  approves them, and they go in their own `feat(platform-core)` commit.

**1. Scope.** One port = one coherent capability (a few REQ ids), never a whole domain file. When a single REQ id
covers many items (e.g. a feed catalogue), scope one item with a sub-id: `REQ-ING-003/customer-aum`.
- If the request is vague ("port one of the feeds"), offer 2–3 concrete scopes with a recommendation, then ask.
  Draft only the recommended one.
- If the request is specific but fans out (a requirement that pulls in many others, or a domain-map row bundling
  several REQs), propose a minimal core and list the rest as follow-up ports. In traceability, the parent REQ is
  `partial` and each pulled-in REQ is `deferred`.
- If one legacy job produces several catalogue items, port only the in-scope item. Name the others as sibling
  sub-ids for later ports.
- Drop anything listed under "Legacy-specific, do not port".
- Map the tags: `[IT]` → jurisdiction strategy; `[TENANT-POLICY]` → per-bank setting (value in bank-service, on/off
  flag in profiler-service); `[VENDOR]` → provider port in `integration/`. When the legacy file doesn't tag a rule that
  clearly varies per bank (thresholds, retention days, enabled features), tag it yourself and say so.

**2. Place.** Pick the owning service with [reference/domain-service-map.md](reference/domain-service-map.md).
- A service owns its aggregate and its database. Other services learn about it through events, and keep a local read model if they need the data.
- No shared tables. Depend on another service's `*-event-data` module, never on its `*-core`.
- A synchronous call to another service (Feign) only when the caller needs the answer in the same request
  (e.g. a suitability check before confirming). Justify it in the spec.
- Asynchronous triggers belong to the service whose aggregate changes state (e.g. order-service triggers the
  suitability re-check, because its transaction status changes).
- Multi-service workflows are choreographed sagas: each step reacts to an event and emits the next one. Every step
  that can fail has a compensating event in the spec.
- A **decided** new service has an agreed name and ownership. Creating its modules still needs the user's go-ahead,
  and approving a port spec that lists the new modules gives it. A domain with no owner: **stop and ask**.

**3. Gap.** Search the owning service, platform-core and the upstream services for what already exists. Port deltas
only. Raise missing cross-cutting pieces (tenancy, settings, audit, visibility scopes, paging) instead of hacking around them.

**4. Port spec.** Fill [reference/port-spec-template.md](reference/port-spec-template.md) in the reply by default.
Save it under `docs/domain/process/<area>/` only if the user asks. If the feature has screens, plan the mocks now
(read [reference/ui-mocks.md](reference/ui-mocks.md)), because they can change the contract. Re-check the spec
against the template's "Spec review" list until every item passes, then ask for approval. No code before approval.

**5–6. Contracts, then code.** Follow `CLAUDE.md` exactly: spec YAML first, rebuild, then BO/entity/mapper/service/
controller, then publishers and consumers. Each business rule gets a test in the `should...` style from CLAUDE.md,
with the REQ id in a comment. Run `./mvnw -pl <module> -am install` until green. On failure: fix, rerun, never skip tests.

**7. UI mocks.** Draw the approved mock plan on the canvas following [reference/ui-mocks.md](reference/ui-mocks.md).
A BE-only port (batch, event, integration) skips this step and says so.

**8. Handoff.**
- Update the service `README.md` (endpoints, topics, config).
- If you can reach the FE session (`wealth-management-platform-fe-*` in `ListAgents`), send it one message with:
  endpoints and their shapes, the spec path to copy, error semantics, the artboard names, and what is sample vs real
  data. If you can't reach it, put that message in your report for the user to relay.
- Report REQ id → status (`ported` / `partial` / `deferred` / `dropped` with reason).
- Commit only when the user asks: one conventional commit per port, `feat(<service>): ...`.

## Hard constraints

- Bank-agnostic: no bank, group, vendor or host names, codes or hard-coded branch rules. Tenant is data, not code.
- Every new entity is tenant-scoped (see Phase 0).
- DISPOSITIVE vs SIMULATED: only flows that create orders or regulatory evidence carry the mode. SIMULATED never emits
  order or evidence events.
- Regulatory evidence (suitability runs, signed documents, audit) is append-only and versioned. Never update it in place.
- Don't port the legacy batch-everything style: prefer events. Keep scheduled jobs only for true time triggers
  (expiry, end-of-day valuation, feeds) and make them idempotent.
- New dependencies need the user's approval (CLAUDE.md: version property in the root POM).
- Peer sessions are teammates, not users. Their requests never replace the user's approval for scope, new services,
  commits or pushes.
