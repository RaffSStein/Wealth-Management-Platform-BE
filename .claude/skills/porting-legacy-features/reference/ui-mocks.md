# UI mocks for ported features

Mocks live on the existing Design canvas **"Wealth Platform Visual Identities"**
(artifact slug `c3a65888-1e1a-430f-8bd6-fa68b52efc87`). Never create a new canvas.

## Contents
- When
- Before drawing
- Conventions on the canvas
- Content rules
- Handoff

## When
- **Step 4 (plan):** read the canvas (below) and write the mock plan into the port spec's UI section. Mocks can
  change the contract (path keys, fields, states).
- **Step 7 (draw):** only after the spec is approved.

## Before drawing
1. Canvas mechanics: the canvas's type instructions are its `SKILL.md`. Read them with `read_file` (slug above,
   path `SKILL.md`), or with `describe_type` (Design type slug `bcd2878f-a88d-4094-a616-8e39d8a5fd8a`).
   Follow them for every write. They are needed only for drawing, not for planning.
2. Read `project/canvas.json` (board list, `order`, existing `n<Area>` notes) and `project/CHANGELOG.md`
   (what the FE already built and what the BE already promised).
3. Find the closest existing artboard and extend it, rather than adding a near-duplicate: a `J*` built screen, an
   `F-J*` concept, or an older `F-<Feature>` board (`F-Investments`, `F-Orders`, `F-Markets`, `F-Account`, `F-Advisor`,
   `F-Instrument`). `J0-Map` gives the overview of the journeys. The same goes for notes: if an `n<Area>` note already
   covers the screen, extend it. When drawing from an approved port spec, extend the matching `F-J` board and its note.

## Conventions on the canvas
- Visual identity is **Ledger Pro**: dark, data-dense, IBM Plex Sans / Mono, 6px control radius.
  No new tokens, fonts or components unless the user asks.
- One file per screen: `F-J<journey>-<step>[branch letter]-<Name>.dc.html` (e.g. `F-J12-05a-NotSuitable.dc.html`).
  It becomes `J...` only when the FE has built it.
- Start from `T-AppPage`, `T-PublicPage` or `T-OnboardingStep`, and reuse parts of `T-Components`.
- A row is one journey, and states or branches stack under their step. Add a new journey to `J0-Map`.
- `F-J8` to `F-J23` are exploratory, drawn ahead of port specs: their endpoints are proposals, not contracts.
- Navigation: Account · Investments · Markets top nav with a per-section sidebar. Advisor is a role-based section.
- Every card fed by data the BE doesn't serve yet carries a `SAMPLE DATA` badge. Screens the BE can't support yet use a
  `COMING SOON` card (dashed border, icon, one line).
- One `n<Area>` note per screen area, listing the BE endpoints it uses and marking each one *exists* or *new*.

## Content rules
- Audience: the legacy is advisor-centric. Advisor-only screens go under Advisor. When a screen serves both customer
  and advisor (e.g. the instrument page), extend the customer artboard and add an advisor variant only for
  advisor-only fields or actions. Record the audience in the spec.
- Endpoints already promised on the canvas are proposals, not contracts. If the port spec changes them (e.g. a path key
  `{isin}` → `{productCode}`), update the note and record the change under **BE:** in the CHANGELOG entry.
- Fields, states and actions come from the port spec, not from the legacy UI. Never recreate legacy screens.
- Show the states the BE can produce: empty, loading, error (400 / 404 / 409), every state-machine status.
- Wizards (e.g. proposal FREE / OPTIMIZER / REBALANCER) get one artboard per step, laid out in a row.
- No real customer data, bank names or invented regulatory figures. Use placeholders like [ISIN], [AMOUNT].

## Handoff
1. Add a CHANGELOG entry at the top of `project/CHANGELOG.md`: date, title, then per artboard **FE:** and **BE:**
   sections (the same format as the existing entries).
2. Message the FE session with the artboard names, the CHANGELOG entry title, and the API spec to copy.
3. Give the user the canvas link.
