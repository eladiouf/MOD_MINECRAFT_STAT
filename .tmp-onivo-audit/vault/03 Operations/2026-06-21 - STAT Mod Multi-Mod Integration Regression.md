---
type: risk
status: Monitoring
severity: S2-High
owner: Verification Cell
date: 2026-06-21
next_review: 2026-06-22
related_projects:
  - STAT Mod
tags:
  - risk
  - statmod
---

# 2026-06-21 - STAT Mod Multi-Mod Integration Regression

## Description

`STAT Mod` now depends on a growing set of combat, perk, race, and magic integration surfaces that can regress each other.

## Impact

An apparently local change can break combat, perk routing, gating, or runtime compatibility across integrated mods.

## Signals

- dedicated integration tests already exist for Epic Fight, Puffish, Tensura, Overgeared, and magic subsystems
- optional-mod support relies on compat layers and mixins

## Mitigation

- execute [[2026-06-21 - STAT Mod Verification Baseline Sweep Mission]]
- keep major integrations behind explicit compat surfaces
- 2026-06-21: baseline `test` and `build` passed on the active branch

## Status Update

Downgraded from `S1-Critical` to `S2-High` after the current branch passed baseline `test` and `build`. The risk remains materially open because:

- no fresh `runClient` validation was recorded in the current studio pass
- mission `M5` still owns Iron's Spellbooks runtime integration tests and save migration checks
- the historical broad completion claim still needs separation from current governed truth

## Escalation

Escalate if verification cannot isolate which integration family is currently failing.
