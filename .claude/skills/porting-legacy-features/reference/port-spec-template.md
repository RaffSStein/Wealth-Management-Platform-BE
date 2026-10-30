# Port spec template

Use this structure. Omit a section only by writing "n/a" and the reason.

```markdown
# Port: <capability name>

**REQ ids:** REQ-<DOMAIN>-NNN[/<item>], ...   **Priority:** MUST | SHOULD | COULD
**Owning service:** <x>-service      **Touches:** <other services, and how: event / sync call>

## Blockers
- Phase 0: tenancy ✗/✓ · per-bank settings ✗/✓/n-a · outbox ✗/✓/n-a · scheduled jobs ✗/✓/n-a
- Step 0 work: skeleton migrations, new modules, missing upstream events (owner + event name)

## Behaviour (from legacy)
- One bullet per business rule, each citing its REQ id.
- State machine (if any): states, transitions, and who or what triggers each one.

## Dropped / changed vs legacy
- What we intentionally don't port, or redesign, and why. Partial REQs: which part is deferred.

## Tags
- [IT] ... → jurisdiction strategy <name>
- [TENANT-POLICY] ... → bank-service setting `<area>.<key>` (type, default) | profiler-service flag `<feature>`
- [VENDOR] ... → port `<Capability>Provider` in integration/<name>
- (inferred) ... → tags you added because the rule clearly varies per bank

## Contracts
### REST (in <x>-api-data.yaml)
| Method | Path | operationId | Request → Response | Errors |
### Events
| Event name | Producer | Consumers ("none yet" if publish-only) | Payload (key fields) | Trigger |
### Sync calls
| Caller → callee | Why it can't be async |
### Compensation
| Failing step | Compensating event / action |

## Data model
- Entities / BOs: key fields, relations, tenant scoping, versioning. Read models fed by other services' events.

## Cross-cutting
- Tenancy, visibility scope, audit actions, DISPOSITIVE/SIMULATED, i18n, paging/export.

## Tests
- One line per business rule → `should...` test name.

## UI
- Audience (customer / advisor / both) and screens → artboard names (new or extended), the `n<Area>` note to add or
  extend, and any change to endpoints already promised on the canvas. Or "BE-only".

## Open questions
- For the user, or for the legacy session (behaviour only).
```

## Spec review (feedback loop)

Re-check before asking for approval. Fix and re-check until all pass:
- [ ] Every in-scope REQ id appears in Behaviour, or in Dropped with a reason.
- [ ] Exactly one owning service. Cross-service data moves only by events, or by a sync call justified in the table.
- [ ] Every saga step that can fail has a compensation row.
- [ ] No other service's `*-core` dependency. No shared table.
- [ ] Schema names are unique platform-wide (`...DTO`, `...Event`). Events are past tense and kebab-case.
- [ ] No bank, vendor, host or legacy package names. Tags are mapped to a setting, strategy or port.
- [ ] Consumers are idempotent. Regulatory records are append-only.
- [ ] Each business rule has a test. Error semantics follow target-architecture.md.
- [ ] Blockers, new modules and new dependencies are listed for the user's approval.
