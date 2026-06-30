---
type: decision
status: Accepted
decision_id: STAT-DEC-001
date: 2026-06-21
area: platform-identity
owners:
  - Founder Desk
  - Executive Orchestrator
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
---

# 2026-06-21 - STAT Mod NeoForge Runtime Truth

## Context

The active local product line for `STAT Mod` is the `neoforge-1.21.1` branch, with `Minecraft 1.21.1`, `NeoForge 21.1.228`, and `Java 21` surfaced directly in the build files. Some public-facing documentation still describes the older Forge 1.20.1 posture.

## Decision

`Onivo Studio` treats the active NeoForge 1.21.1 branch as the runtime source of truth for current `STAT Mod` operations. Any release, verification, or publishing work must align with that branch reality until a different branch is explicitly elevated.

## Consequences

- release-facing documentation must be refreshed before credible publication
- verification and dependency checks should target the NeoForge 1.21.1 line
- stale public identity becomes an explicit risk, not an ambiguity

## Follow-up

- [[2026-06-21 - STAT Mod Release Identity Alignment Mission]]
- [[2026-06-21 - STAT Mod Release Readiness Track]]
