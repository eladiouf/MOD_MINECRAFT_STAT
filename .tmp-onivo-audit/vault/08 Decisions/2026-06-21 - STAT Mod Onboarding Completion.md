---
type: decision
status: Accepted
decision_id: STAT-DEC-004
date: 2026-06-21
area: studio-operations
owners:
  - Executive Orchestrator
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
  - onboarding
---

# 2026-06-21 — STAT Mod Onboarding Completion

## Context

The project onboarding playbook in `operating-system/project-onboarding-playbook.md` defines a 6-step flow:

1. adapter package in `projects/<project-id>/`
2. local `.onivo-project/` bridge inside the project repo
3. project note in the vault
4. first strategy review
5. initial decision, missions, risks, release track
6. dashboards surface the project

Five of six are present:

- step 1 — `projects/statmod/` with dashboard, contracts, product, architecture, execution, quality, release, history, links
- step 3 — `vault/04 Projects/STAT Mod.md`
- step 4 — `vault/02 Strategy/2026-06-21 - STAT Mod Weekly Strategy Review.md`
- step 5 — three closed missions, one executing mission, one decision (`STAT-DEC-001`), release track noted
- step 6 — dashboards reference the project surfaces

Step 2 (`.onivo-project/` bridge inside the project repo) is partially present in commit `0a92379` (`docs: add Onivo project bridge`) and `c5b50fe` (`docs: link Onivo bridge to live studio surfaces`).

## Decision

`STAT Mod` is declared **onboarded into Onivo Studio**. The onboarding is closed. Any remaining surface gaps fall under normal operating cadence, not onboarding work.

The plenary session of 2026-06-21 is recognized as the first plenary-grade studio operation on `STAT Mod` post-onboarding.

## Consequences

- the studio may now route `STAT Mod` work through standard mission templates without onboarding rituals
- `project-home.md` will be amended to reflect the current realistic state (the previous `Integration Complete` claim is challenged by open missions `M3`, `M5`, `M6`, `M9`)
- studio scoreboards in `delivery/studio-scoreboards.md` must surface `STAT Mod` as the first live project

## Follow-up

- mission `M3` updates the project `CLAUDE.md` and may revisit `project-home.md` phrasing
- studio scoreboards refresh in next Daily Ops Review
