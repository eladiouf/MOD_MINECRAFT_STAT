# Stafffix and Mahou Tsukai Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `stafffix` and `Mahou Tsukai` from the active project runtime and live project references without disturbing unrelated integrations.

**Architecture:** Treat `libs/` as the runtime source of truth, remove the unwanted jars there, then clean the small number of active build and current-doc references that still mention Mahou support. Keep historical plans/specs as archives unless they misrepresent the current state.

**Tech Stack:** Java 21, NeoForge 1.21.1 Gradle build, local `libs/` jar-managed runtime, JUnit 5 verification via Gradle

---

## File Structure

- Modify: `build.gradle`
  - Remove the Mahou-specific jar exclusion because Mahou is no longer part of the supported runtime model
- Modify: `CLAUDE.md`
  - Remove or tighten any current-facing wording that still implies Mahou is an expected live integration target or pending cleanup item
- Delete: `libs/stafffix-1.0.0.jar`
  - Remove the direct crash source
- Delete: `libs/mahoutsukai-1.21.1-v1.36.27.jar`
  - Remove obsolete Mahou runtime dependency
- Delete: `libs/mahoutsukai-1.21.1-v1.36.8.jar`
  - Remove obsolete duplicate Mahou runtime dependency
- Test: repository-wide search and `.\gradlew.bat build`

### Task 1: Remove runtime jars

**Files:**
- Delete: `libs/stafffix-1.0.0.jar`
- Delete: `libs/mahoutsukai-1.21.1-v1.36.27.jar`
- Delete: `libs/mahoutsukai-1.21.1-v1.36.8.jar`

- [ ] **Step 1: Verify the jars exist before removal**

Run: `Get-ChildItem -Force libs | Sort-Object Name | Select-Object Name`
Expected: output includes `stafffix-1.0.0.jar` and both `mahoutsukai` jars

- [ ] **Step 2: Remove the jars**

Run: `Remove-Item -LiteralPath 'libs\stafffix-1.0.0.jar','libs\mahoutsukai-1.21.1-v1.36.27.jar','libs\mahoutsukai-1.21.1-v1.36.8.jar'`
Expected: command succeeds with no path errors

- [ ] **Step 3: Verify the jars are gone**

Run: `Get-ChildItem -Force libs | Sort-Object Name | Select-Object Name`
Expected: no `stafffix` or `mahoutsukai` jars remain

- [ ] **Step 4: Commit**

```bash
git add -A -- libs
git commit -m "chore(runtime): remove stafffix and mahou jars"
```

### Task 2: Remove active build and live-doc references

**Files:**
- Modify: `build.gradle`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Write the failing search check**

Run: `rg -n "stafffix|mahoutsukai|Mahou Tsukai|integration/mahou" build.gradle CLAUDE.md`
Expected: finds the active references that must be cleaned

- [ ] **Step 2: Remove only current-facing active references**

```gradle
implementation fileTree(dir: 'libs', include: '*.jar', exclude: ['elementals*.jar'])
```

```md
- remove the residual `integration/mahou/` cleanup note if the folder is no longer part of active work
- keep historical explanation that Mahou was removed only if it clearly states removal rather than support
```

- [ ] **Step 3: Re-run the search check**

Run: `rg -n "stafffix|mahoutsukai|integration/mahou" build.gradle CLAUDE.md`
Expected: no active build reference remains; only acceptable historical wording may remain if still explicitly marked removed

- [ ] **Step 4: Commit**

```bash
git add build.gradle CLAUDE.md
git commit -m "chore(docs): remove active mahou support references"
```

### Task 3: Verify no active source/test references remain

**Files:**
- No mandatory source edits expected unless the search reveals live Mahou usage

- [ ] **Step 1: Search active code and tests**

Run: `rg -n "stafffix|mahoutsukai|Mahou Tsukai|mahou" src/main src/test`
Expected: either no results or only intentionally absent historical placeholders outside active code

- [ ] **Step 2: If search is clean, document that no source removal was needed**

Expected: no code changes required because active Mahou integration has already been removed from `src/`

- [ ] **Step 3: If search reveals active references, remove them and re-run the search**

Run: `rg -n "stafffix|mahoutsukai|Mahou Tsukai|mahou" src/main src/test`
Expected: clean result after removal

### Task 4: Final verification

**Files:**
- No additional code files required

- [ ] **Step 1: Search the workspace for current-state references**

Run: `rg -n "stafffix|mahoutsukai" build.gradle CLAUDE.md src/main src/test docs/superpowers/specs/2026-06-25-mod-compatibility-matrix.md`
Expected: no active runtime/build reference remains; compatibility matrix may still say `REMOVED`

- [ ] **Step 2: Run the full build**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Review git status for accidental noise**

Run: `git status --short`
Expected: only intended removal changes are staged or committed; unrelated logs and temporary directories stay untouched

## Self-Review

- **Spec coverage:** The plan removes runtime jars, cleans active build/live-doc references, verifies source cleanliness, and ends with a full build.
- **Placeholder scan:** No `TODO`, `TBD`, or vague “clean later” steps remain.
- **Type consistency:** The plan consistently treats `libs/` as runtime source of truth and distinguishes live docs from historical archives.
