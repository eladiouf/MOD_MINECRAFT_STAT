---
type: risk
status: Open
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

# 2026-06-21 - STAT Mod Dependency Floor Drift

## Description

The active runtime depends on a minimum NeoForge and optional mod stack that can drift faster than the release-facing docs.

## Impact

Players can install incompatible versions or miss new minimum requirements.

## Signals

- `gradle.properties` sets `neo_version=21.1.228`
- recent user feedback indicated several mods require `NeoForge 21.1.228 or above`

## Mitigation

- execute [[2026-06-21 - STAT Mod Verification Baseline Sweep Mission]]
- feed the confirmed floors into [[2026-06-21 - STAT Mod Release Readiness Track]]

## Escalation

Escalate if dependency floors are inconsistent across code, runtime, and release docs.
