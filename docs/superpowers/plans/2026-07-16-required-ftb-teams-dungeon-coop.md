# Required FTB Teams Dungeon Co-op Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Forge 1.20.1 Trial Dungeon use FTB Teams as its mandatory and authoritative cooperative party system.

**Architecture:** Add the real FTB Teams API to the development classpath and required mod metadata. Centralize strict server-team resolution in `FTBTeamsBridge`, then use a pure admission policy plus team-filtered recipient selection throughout dungeon entry, progression, rewards, death handling, and boss cooldowns.

**Tech Stack:** Java 17, Forge 47.4.10, Minecraft 1.20.1, FTB Teams Forge 2001.3.2, JUnit Jupiter 5.10.2.

## Global Constraints

- FTB Teams is mandatory on both logical sides.
- Missing/unready FTB Teams fails closed for dungeon cooperation.
- Floor 0 stays public; challenge floors admit one team only.
- No forced group teleportation.
- Existing dungeon scaling, loot, geometry, and persistent progression remain unchanged.

---

### Task 1: Dependency and bridge contract

**Files:** `build.gradle`, `gradle.properties`, `src/main/resources/META-INF/mods.toml`, `FTBTeamsBridge.java`, and focused contract/unit tests.

- [ ] Write tests requiring mandatory `ftbteams`, the direct API import, and strict team-ID comparison.
- [ ] Run the focused tests and confirm RED.
- [ ] Add FTB Teams/FTB Library dependencies and implement the direct bridge.
- [ ] Run focused tests and confirm GREEN.

### Task 2: Exclusive team admission

**Files:** `DungeonFloorAdmission.java`, its test, `DungeonTeleportHandler.java`, and language JSON.

- [ ] Write admission tests for empty, same-team, rival-team, and unavailable-manager cases.
- [ ] Run them and confirm RED.
- [ ] Implement the policy and enforce it before challenge-floor generation.
- [ ] Run focused tests and confirm GREEN.

### Task 3: Team-filter all cooperative effects

**Files:** `DungeonProgress.java`, `DungeonPoints.java`, `DungeonRespawnHandler.java`, and `DungeonBossHandler.java`.

- [ ] Add contract tests asserting every cooperative recipient path uses `FTBTeamsBridge.sameTeam` or `teammatesOnFloor`.
- [ ] Run them and confirm RED.
- [ ] Filter completion, assists, death retry protection, and boss cooldown recipients.
- [ ] Run focused tests and confirm GREEN.

### Task 4: Verify and document

**Files:** `CHANGELOG.md` and all changes above.

- [ ] Run `git diff --check`.
- [ ] Run the complete test suite.
- [ ] Run `clean build` and inspect the produced JAR metadata.
- [ ] Record the required FTB Teams co-op behavior in the changelog and commit the completed work.

