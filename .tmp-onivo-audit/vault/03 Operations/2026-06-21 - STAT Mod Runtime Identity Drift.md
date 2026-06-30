---
type: risk
status: Monitoring
severity: S2-High
owner: Product Cell
date: 2026-06-21
next_review: 2026-06-23
related_projects:
  - STAT Mod
tags:
  - risk
  - statmod
---

# 2026-06-21 - STAT Mod Runtime Identity Drift

## Description

The core release-facing identity has been realigned to `NeoForge 1.21.1`, but legacy technical documentation can still carry older Forge 1.20.1 framing.

## Impact

Players, testers, and future agents can act on the wrong runtime assumptions.

## Signals

- `README.md` is now aligned with the active branch
- some technical docs such as `ARCHITECTURE.md` still use older Forge 1.20.1 wording
- `gradle.properties` and `build.gradle` target NeoForge 1.21.1

## Mitigation

- keep [[2026-06-21 - STAT Mod Release Identity Alignment Mission]] closed as the release-facing fix
- keep [[2026-06-21 - STAT Mod NeoForge Runtime Truth]] as the current operating decision

## Escalation

Escalate if code, metadata, and release surfaces still disagree after the alignment pass.
