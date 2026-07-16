# Required FTB Teams Dungeon Co-op Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Forge 1.20.1 Trial Dungeon use FTB Teams as its mandatory and authoritative cooperative party system.

**Architecture:** Keep FTB Teams mandatory for team identity and assist rewards, while treating each physical floor as one public shared encounter. Entry and completion operate on all players present; only assists use `FTBTeamsBridge.teammatesOnFloor`.

**Tech Stack:** Java 17, Forge 47.4.10, Minecraft 1.20.1, FTB Teams Forge 2001.3.2, JUnit Jupiter 5.10.2.

## Global Constraints

- FTB Teams is mandatory on both logical sides.
- Missing/unready FTB Teams never blocks dungeon entry.
- Floor 0 and challenge floors admit multiple teams.
- No forced group teleportation.
- Existing dungeon scaling, loot, geometry, and persistent progression remain unchanged.

---

### Task 1: Dependency and bridge contract

**Files:** `build.gradle`, `gradle.properties`, `src/main/resources/META-INF/mods.toml`, `FTBTeamsBridge.java`, and focused contract/unit tests.

- [ ] Write tests requiring mandatory `ftbteams`, the direct API import, and strict team-ID comparison.
- [ ] Run the focused tests and confirm RED.
- [ ] Add FTB Teams/FTB Library dependencies and implement the direct bridge.
- [ ] Run focused tests and confirm GREEN.

### Task 2: Public multi-team admission

**Files:** `DungeonTeleportHandler.java`, `DungeonFtbTeamsWiringContractTest.java`, and language JSON.

- [ ] Rewrite admission tests so rival teams and an unavailable manager never block entry.
- [ ] Run them and confirm RED.
- [ ] Remove team admission enforcement and obsolete refusal translations.
- [ ] Run focused tests and confirm GREEN.

### Task 3: Share encounters while keeping team-only assists

**Files:** `DungeonProgress.java`, `DungeonPoints.java`, `DungeonRespawnHandler.java`, and `DungeonBossHandler.java`.

- [ ] Add contract tests asserting completion, death continuity, and boss cooldowns use all floor players while assists use `teammatesOnFloor`.
- [ ] Run them and confirm RED.
- [ ] Share completion and boss cooldowns with all present players, preserve a wave while anyone remains, and retain team-filtered assists.
- [ ] Run focused tests and confirm GREEN.

### Task 4: Verify and document

**Files:** `CHANGELOG.md` and all changes above.

- [ ] Run `git diff --check`.
- [ ] Run the complete test suite.
- [ ] Run `clean build` and inspect the produced JAR metadata.
- [ ] Record the required FTB Teams co-op behavior in the changelog and commit the completed work.
