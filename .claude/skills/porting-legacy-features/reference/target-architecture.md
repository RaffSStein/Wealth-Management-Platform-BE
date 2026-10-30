# Target architecture checklist for ports

`CLAUDE.md` is authoritative. This file lists only what a port most often gets wrong.

## Contents
- Contracts
- Events
- Layering and patterns
- Legacy → target translations

## Contracts
- REST spec in `<x>-api-data/<x>-api-data.yaml`. Paths are prefixed by domain. Every operation has `operationId`, summary, description and tags.
  Models are suffixed `DTO`, and schema names are unique platform-wide.
- Event payloads in `<x>-event-data/<x>-event-data.yaml`, suffixed `Event`.
- Reuse `ErrorResponse` and `ErrorCategory` from platform-core `api-data.yaml`. It has no shared `Pageable` yet
  (only bank-api-data defines one). The first port that needs paging proposes moving it to platform-core.
- Error semantics (what `GlobalControllerExceptionHandler` maps today): 400 validation, and business-rule rejections such
  as "MiFID expired", carried by a specific error code. 403 access denied. 404 `OBJECT_NOT_FOUND`. 409 invalid state
  transition or version conflict. There is no 422 mapping: don't use 422 unless the port adds it to platform-core.
  Write the errors in the spec.
- A provider or downstream technical failure on a sync path has no dedicated mapping yet (it becomes 500). If the port
  needs 502/503, it proposes the platform-core mapping in the spec. A rule that can't be evaluated (e.g.
  `NOT_COMPUTABLE`) is a business outcome in a 200 response, not an error.

## Events
- Topic `${env}.${tenant}.<domain>.<event-name>`, event names kebab-case past tense (`proposal-signed`).
- Publisher `<EventName>EventPublisher`, consumer `<EventName>EventConsumer` extending `WMPBaseEventConsumer`, gated by `@ConditionalOnProperty`.
- Consumers are idempotent: dedupe on event id or a business key, because Kafka redelivers.
- An event carries ids plus the fields consumers need. It never carries a full entity, and never PII beyond what is needed.
- Bulk feeds (thousands of rows or more): emit chunked events (`loadId`, `chunkSeq`, rows) and then one
  `...-completed` event with the totals. Consumers stage the chunks and promote the snapshot only on completion.
  If a single run is too large even for chunks, publish a claim-check (a reference to the staged data) instead.
- A state change and its event are published together: if the transaction rolls back, nothing is published.
  Use the platform-core transactional outbox once it exists. Which ports need it is defined in SKILL.md Phase 0. Other
  ports publish directly and note the risk in the port spec.

## Layering and patterns
- DTO → controller mapper → BO → service → entity mapper → entity → repository. No DTOs in services.
- State machines and multi-step flows use a handler interface plus a dispatcher (`MifidWorkflowDispatcher`, `OnboardingStepDispatcher`).
  The ~27 legacy order states become handlers, not a `switch`.
- Rule sets (suitability, alerts, data-quality checks) are a `List<Rule>` of `@Component`s. Each rule is unit-tested on its own.
- Versioned or regulatory records: insert a new version, deprecate the old one (see the MiFID filling lifecycle).
- `@Transactional` only on service methods, and transactions stay short. No remote calls inside a transaction.

## Legacy → target translations

| Legacy | Target |
|---|---|
| Oracle table + DAO per screen | Aggregate per service, JPA entity, derived queries / `@EntityGraph` |
| Nightly batch moving data between modules | Domain event + consumer; batch only for true time triggers |
| Bank code / branch code columns everywhere | Tenant / branch from `WMPContext` + tenant-scoped base entity |
| JSON config blobs in a master table | Typed per-bank settings in `bank-service`, feature flags in `profiler-service`, cached reader in platform-core |
| Session-polling SSO servlets | platform-core JWT resource server |
| Mock web servers for providers | WireMock / Postman contract mocks per provider port |
| Fixed-width / record-buffer formats | Adapter in `integration/` translating to events or DTOs |
| Excel export per controller | Shared streamed XLSX helper |
