# Validation Surface

## Verification Surfaces

- `src/test/java/` targeted unit coverage
- Gradle `test`
- Gradle `build`
- Gradle `runClient` when runtime behavior must be checked
- `vault/03 Operations/2026-06-21 - STAT Mod Verification Baseline Sweep Mission.md`
- `vault/07 Publishing/2026-06-21 - STAT Mod Release Readiness Track.md`

## Known Pressure

- external mod APIs and mixins can drift
- large integration slices can leave documentation behind
- local logs and scratch artifacts can obscure signal during audits

## Current Baseline

- `.\gradlew.bat test` passed on the active branch — 70+ tests across all integration surfaces
- `.\gradlew.bat build` passed on the active branch
- NeoGradle warns that `Run.getProgramArguments()` is deprecated
- no fresh `runClient` evidence was collected in this studio pass
- broad integration build/test baseline is green; runtime-specific Iron's validation and historical-claim audits still remain
