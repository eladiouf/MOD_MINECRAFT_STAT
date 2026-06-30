---
type: release
status: Preparing
project: STAT Mod
surface: NeoForge 1.21.1 release line
target_window: current active branch alignment
owners:
  - Publishing & Release Cell
  - Verification Cell
tags:
  - release
  - statmod
---

# 2026-06-21 - STAT Mod Release Readiness Track

## Scope

Current release-preparation surface for the active `NeoForge 1.21.1` line.

## Readiness

- Build: `.\gradlew.bat build` passed on the active branch baseline
- QA: `.\gradlew.bat test` passed on the active branch baseline
- Runtime identity: aligned in `README.md` and metadata through project commit `e0e1073`
- Store assets: not reviewed on the active branch line
- Changelog: not curated for the current release line
- Runtime validation: no fresh `runClient` evidence recorded in this studio pass

## Risks

- [[2026-06-21 - STAT Mod Runtime Identity Drift]]
- [[2026-06-21 - STAT Mod Dirty Worktree Masks Release Readiness]]
- [[2026-06-21 - STAT Mod Dependency Floor Drift]]

## Launch Notes

Do not treat the branch as release-ready until runtime truth, verification evidence, and dependency floors are aligned.

The main public release identity drift is no longer the primary blocker. Verification and dependency confidence now dominate the release picture.

Current technical note:

- NeoGradle emits a deprecation warning about `Run.getProgramArguments()`, recommending `getArguments()`
