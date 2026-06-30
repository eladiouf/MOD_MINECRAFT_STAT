---
type: decision
status: Accepted
decision_id: STAT-DEC-002
date: 2026-06-21
area: integration-policy
owners:
  - Executive Orchestrator
  - Product Cell
  - Strategy Council
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
  - epicfight
---

# 2026-06-21 — STAT Mod Epic Fight Integration Status

## Context

The project `CLAUDE.md` for `STAT Mod` on the NeoForge 1.21.1 branch explicitly forbids Epic Fight:

```
Absolument PAS : Epic Fight ou ses addons
```

The reasoning at the time was that Epic Fight had no NeoForge 1.21.1 port. Since then:

- the active codebase already contains an `integration/epicfight/` package
- the multi-mod master plan `2026-06-16-multi-mod-integration-master-plan.md` reserves 7 files for Epic Fight integration
- the project dashboard claims all 7 phases of integration are complete

This produces a constitutional contradiction between the project rulebook and the code shipping in the same branch.

## Decision

Epic Fight is reclassified as an **experimental optional integration** of `STAT Mod`. Concretely:

- the `CLAUDE.md` prohibition on Epic Fight is removed
- Epic Fight is declared an optional, version-gated runtime surface — STAT Mod does not require it
- Epic Fight integration code is allowed to ship in the codebase but is not part of the initial public release surface
- the master plan `integration/epicfight/` package remains supported as ongoing exploratory work
- a compatibility matrix entry must list the exact supported Epic Fight version under mission `M10`

This is option **C** of the three options considered:

- A — re-enforce prohibition and rip out code (rejected: discards shipped work and tests)
- B — promote Epic Fight to first-class supported integration (rejected: locks studio into Epic Fight upstream cadence)
- C — recognize Epic Fight as experimental optional (accepted: preserves work, preserves flexibility)

## Consequences

- mission `M3` will amend `CLAUDE.md` to remove the prohibition and add an integration status table
- Epic Fight bugs are now triaged as integration issues, not constitutional violations
- public release notes must mark Epic Fight support as experimental until a Release Readiness Review reclassifies it
- the studio reserves the right to demote Epic Fight to `removed` if upstream NeoForge porting stalls

## Follow-up

- mission `M3` to amend `CLAUDE.md`
- mission `M10` to add Epic Fight to the compatibility matrix
- weekly Strategy Council to revisit promotion to first-class after Phase 1 magic ships
