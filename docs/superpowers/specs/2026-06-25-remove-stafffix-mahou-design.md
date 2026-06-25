# STAT Mod - Stafffix and Mahou Tsukai Removal Design

**Date:** 2026-06-25  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Approved design executed inline per user instruction

## 1. Goal

Remove `stafffix` and every active `Mahou Tsukai` runtime dependency from the project so:

- the modpack no longer crashes on `stafffix requires mahoutsukai`
- the current project stops treating `Mahou Tsukai` as a live integration target
- the build remains green after the cleanup

This is a removal slice, not a replacement feature.

## 2. Scope

### In Scope

- remove `stafffix` from local runtime dependencies
- remove `mahoutsukai` jars from local runtime dependencies
- remove active source/test/build references that still assume `Mahou Tsukai` is installed or supported
- update live docs that still present `Mahou Tsukai` as part of the current stack
- verify the project with search and `.\gradlew.bat build`

### Out of Scope

- rewriting every historical design document
- refactoring unrelated magic systems
- replacing Mahou content with a new magic mod in this slice
- cleaning unrelated jars or optional mods

## 3. Problem Statement

The current runtime still contains:

- `stafffix-1.0.0.jar`
- multiple `mahoutsukai` jars

`stafffix` hard-requires `mahoutsukai`, and `mahoutsukai` is no longer part of the intended project direction. That leaves the workspace in a contradictory state:

- docs and recent decisions say Mahou was removed
- runtime still contains Mahou-related jars
- client crash logs confirm the dependency mismatch

The fix must align runtime, source, and current documentation.

## 4. Design Choice

Three candidate approaches were considered:

1. runtime-only removal
2. complete strict removal
3. complete removal plus historical doc rewrite

This design chooses **complete strict removal**.

Reason:

- runtime-only cleanup would still leave active source and docs lying about current support
- historical full rewrite is unnecessary churn
- strict removal solves the real crash and brings the active codebase back in sync

## 5. Runtime Design

The local `libs/` directory is the current source of truth for third-party runtime jars in this workspace.

The removal will:

- delete `stafffix*.jar`
- delete `mahoutsukai*.jar`

No replacement jar is introduced in this slice.

## 6. Source and Build Design

The cleanup will search for active references across:

- `src/main`
- `src/test`
- `build.gradle`
- live project docs

If Mahou-related source is still active, it is removed. If source is already gone but tests or config still mention it as active behavior, those references are removed or rewritten to match the current project model.

`build.gradle` should not keep Mahou-specific behavior unless it still serves a real remaining dependency pattern.

## 7. Documentation Design

Only live or current-facing docs are updated.

Rules:

- current source-of-truth docs should no longer present `Mahou Tsukai` as an active supported mod
- historical plans, audits, and archived specs may remain as historical artifacts
- if a current compatibility matrix exists, it should reflect `Mahou Tsukai` as removed

## 8. Validation Strategy

Validation will be done in this order:

1. search for `stafffix`, `mahou`, and `mahoutsukai` references
2. remove runtime jars and active references
3. rerun searches to verify only acceptable historical references remain
4. run `.\gradlew.bat build`

`runClient` is intentionally not part of this slice because the user previously requested that it not be launched.

## 9. Error Handling

- if the build fails after removal, fix only fallout directly caused by the removal
- if references remain only in clearly historical documents, leave them unless they misrepresent current support
- if another active dependency unexpectedly requires Mahou, remove or reclassify that dependency rather than restoring Mahou

## 10. Acceptance Criteria

The slice is complete when:

- `stafffix` jar is gone from the runtime dependency folder
- `mahoutsukai` jars are gone from the runtime dependency folder
- active source/build references no longer treat Mahou as supported
- live docs no longer present Mahou as an active integration target
- `.\gradlew.bat build` succeeds

## 11. Self-Review

- no placeholders remain
- scope is bounded to removal and fallout repair
- runtime, source, and docs are aligned around the same project reality

*Drafted on 2026-06-25. Ready for implementation handoff and inline execution.*
