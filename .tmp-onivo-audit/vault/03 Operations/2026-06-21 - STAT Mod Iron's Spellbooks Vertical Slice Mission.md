---
type: mission
status: Planned
mission_type: Build Mission
owner: Build Cell
target_project: STAT Mod
allowed_surface: magic integration code, tests, plan execution, Puffish bridge updates
success_condition: the approved Iron's Spellbooks vertical slice moves through plan, build, and verification as a governed track
verification_path: plan execution evidence plus targeted tests and build evidence
escalation_rule: escalate if the vertical slice requires a second conflicting source of truth for magic progression
priority: P1
next_review: 2026-06-22
related_projects:
  - STAT Mod
tags:
  - mission
  - statmod
  - magic
---

# 2026-06-21 - STAT Mod Iron's Spellbooks Vertical Slice Mission

## Scope

Advance the unified magic tree work using `Iron's Spellbooks` as the runtime spell layer while keeping `STAT Mod` as the progression authority.

## Constraints

- no second independent progression track
- preserve native upstream UX where possible
- keep release and verification governance explicit

## Deliverables

- progress against the existing Iron's Spellbooks plan
- updated compat and test surfaces
- clear verification and release implications

## Verification

Track implementation evidence against the approved spec/plan and feed results into the verification baseline.

## Escalation Notes

Escalate if addon pressure or runtime behavior pushes toward fragile duplicated systems.
