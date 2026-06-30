---
type: mission
status: Completed
mission_type: Release Mission
owner: Product Cell
target_project: STAT Mod
allowed_surface: release-facing docs, metadata, release notes, adapter references
success_condition: release-facing identity matches the active NeoForge 1.21.1 branch
verification_path: doc audit plus release track update
escalation_rule: escalate if runtime truth is ambiguous across code, metadata, and public docs
priority: P1
next_review: 2026-06-21
related_projects:
  - STAT Mod
tags:
  - mission
  - statmod
  - release
---

# 2026-06-21 - STAT Mod Release Identity Alignment Mission

## Scope

Align `README`, release-facing wording, and studio release surfaces with the actual `NeoForge 1.21.1` product line.

## Constraints

- do not publish inaccurate claims
- do not assume the old Forge 1.20.1 identity is still valid for active work

## Deliverables

- documented runtime truth in the studio
- refreshed release-facing identity surfaces
- updated release readiness track

## Verification

Check `README.md`, build metadata, and [[2026-06-21 - STAT Mod Release Readiness Track]] for consistency.

## Outcome

Completed through project commit `e0e1073` in `MOD_MINECRAFT_STAT`.

The release-facing `README.md` now reflects:

- `Minecraft 1.21.1`
- `NeoForge 21.1.228 or above`
- `Java 21`
- the active `neoforge-1.21.1` development branch

`gradle.properties` was also aligned to `mod_license=MIT` to match the repository license file.

## Escalation Notes

Escalate if branch intent, dependency floor, or supported mod stack is still unclear after inspection.
