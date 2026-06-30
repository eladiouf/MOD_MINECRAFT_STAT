---
type: decision
status: Accepted
decision_id: STAT-DEC-003
date: 2026-06-21
area: intelligence-process
owners:
  - Innovation & Research Lead
  - Verification Cell
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
  - postmortem
---

# 2026-06-21 — STAT Mod Mahou + Elementals Removal Postmortem Opened

## Context

Commit `22f7cff` removed both the Mahou Tsukai and Elementals integrations from the active codebase. No postmortem accompanied the removal. The studio Postmortem Standard requires major incidents or repeated process failures to be captured. Voluntary removal of two integrations after non-trivial implementation work qualifies as a process event worth capturing.

The studio also notes a related signal: commits `328f155`, `35d0ccf`, `dd2d74a`, `fffaabf`, `ee18f1c` invested substantial work in Elementals reward provenance and ownerless damage attribution shortly before the removal commit.

## Decision

A postmortem is opened for the Mahou + Elementals removal. The Innovation & Research Lead owns the postmortem authorship. The Verification Cell owns the code sweep for orphaned references.

Postmortem deliverable lives at:

`vault/09 Postmortems/2026-06-21 - STAT Mod Mahou Elementals Removal.md`

It must follow `intelligence/postmortem-standard.md` and answer:

- what happened
- impact (player-facing, codebase, tests)
- root cause
- why the studio allowed the integrations to be invested in then removed
- corrective action
- policy, template, or playbook updates
- reusable patterns extracted into `intelligence/pattern-library.md`

## Consequences

- studio backlog gains mission `M6`
- code sweep mission scans for orphaned imports, references, registry entries, locale keys, and resource files left by the removal
- if the sweep finds orphans they are removed under mission `M6.1`

## Follow-up

- `vault/09 Postmortems/2026-06-21 - STAT Mod Mahou Elementals Removal.md` created in the same session
- pattern library update follows postmortem closure
