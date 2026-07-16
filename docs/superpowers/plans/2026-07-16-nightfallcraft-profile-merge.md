# NightfallCraft Profile Merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Merge the validated `test-vrai` RPG mods into NightfallCraft while preserving the pack and making Stat Mod load on Forge 47.4.4.

**Architecture:** NightfallCraft remains authoritative. A primary-mod-ID comparison selects only absent JARs, with an explicit Iron's Spellbooks upgrade and a freshly built Stat Mod artifact. A timestamped inventory and replacement backup makes the deployment reversible.

**Tech Stack:** Forge 1.20.1, Gradle 8.8, PowerShell, JAR `mods.toml` inspection.

## Global Constraints

- Target loader is Forge 47.4.4.
- Do not overwrite target configs, saves, options, resource packs, or shader packs.
- Add 29 absent primary mod IDs and retain target copies for ordinary duplicates.
- Replace Iron's Spellbooks 3.15.4 with 3.16.2.
- Deploy only a freshly verified Stat Mod JAR.

---

### Task 1: Forge loader compatibility

**Files:**
- Modify: `gradle.properties`
- Modify: `src/main/resources/META-INF/mods.toml`
- Test: `src/test/java/tong/statmod/ForgeDependencyContractTest.java`

- [ ] Add a contract assertion requiring the Forge range to start at 47.4.4.
- [ ] Run the focused test and confirm it fails against the current 47.4.10 declaration.
- [ ] Change the build and metadata Forge floor to 47.4.4.
- [ ] Run the focused test and confirm it passes.
- [ ] Run `gradlew clean build` and confirm zero failures.

### Task 2: Reversible profile merge

**Files:**
- Create: target `statmod-backups/<timestamp>/before-mods-sha256.csv`
- Create: target `statmod-backups/<timestamp>/added-mods.txt`
- Create: target `statmod-backups/<timestamp>/after-mods-sha256.csv`
- Replace: target Iron's Spellbooks JAR
- Add: 29 absent JARs selected by primary mod ID

- [ ] Hash the original 196 JARs and back up Iron's Spellbooks 3.15.4.
- [ ] Copy absent source JARs, using the fresh build artifact for Stat Mod.
- [ ] Remove only the replaced Iron's Spellbooks 3.15.4 JAR.
- [ ] Copy Iron's Spellbooks 3.16.2.

### Task 3: Final dependency audit

**Files:**
- Create: target `statmod-backups/<timestamp>/merge-report.txt`

- [ ] Parse every final JAR's primary mod ID and report duplicates.
- [ ] Verify Stat Mod, Epic Fight, Puffish Attributes, Iron's Spellbooks, Lootr, and FTB Teams.
- [ ] Verify the final count is 225 JARs.
- [ ] Record final hashes and rollback instructions.

