---
type: decision
status: Accepted
decision_id: STAT-DEC-006
date: 2026-06-21
area: delivery-sequencing
owners:
  - Executive Orchestrator
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
  - delivery
---

# 2026-06-21 — STAT Mod Mission Prioritization Sequence

## Context

The plenary session generated 9 mission packages. Without a sequence the studio risks parallel execution that re-creates worktree pollution and inconsistent runtime state.

## Decision

The following execution order is locked for the next operating cycle. The Executive Orchestrator may interrupt the sequence only on a Verification Cell red flag or a Founder directive.

| Order | Mission | Owner | Why first/why later |
|---|---|---|---|
| 1 | `M3` | Product Cell | Resolves the constitutional contradiction. Unblocks honest documentation for every other mission. Low effort. |
| 2 | `M6` | Innovation & Research Lead + Verification Cell | Fast intelligence win. Captures Mahou + Elementals lessons before they decay. Surfaces orphaned code that `M2` must clean. |
| 3 | `M4` | Build Cell | The active value-generating mission. Iron's Spellbooks Phase 1 is the closest deliverable. Must continue. |
| 4 | `M1` | Verification Cell | Audits whether residual Forge 1.20.1 wiring still exists. Required before any release readiness review. |
| 5 | `M9` | Verification Cell | Re-runs the 10 issues claimed fixed in `065d57a`. Trust-but-verify the prior audit. |
| 6 | `M5` | Verification Cell | Integration tests against real Iron's Spellbooks 3.16.1 runtime plus save migration tests. |
| 7 | `M2` | Build Cell | Worktree cleanup after the postmortem and audit reveal what is actually orphaned. |
| 8 | `M8` | Product Cell | `.opencode/plans/` deprecation notice. Pure documentation, can wait. |
| 9 | `M10` | Product Cell | Mod compatibility matrix. Useful for release prep, not blocking Phase 1. |

## Consequences

- `04-execution/current-initiatives.md` is updated to surface this ordering
- the studio scoreboards mark `M3` and `M6` as the only two missions in `Active` state simultaneously to avoid parallel chaos
- `M4` opens as soon as `M3` closes since it has no blockers

## Follow-up

- update `04-execution/current-initiatives.md`
- update `delivery/portfolio-backlog.md`
