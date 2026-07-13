# Dungeon Monster Health Scaling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scale every authorized dungeon monster from 3x health at floor 1 to 15x at floor 100, with role bonuses and a 25x Abyss cap.

**Architecture:** Keep the pure multiplier curve in `DungeonMobScaling` and apply one stable `MAX_HEALTH` modifier after L2 initialization. Mark queued mini-bosses explicitly so role scaling does not depend on modded entity types.

**Tech Stack:** Java 21, NeoForge 1.21.1, JUnit 5, Gradle.

## Global Constraints

- Apply only to authorized dungeon mobs.
- Do not change monster damage, player attributes, encounter counts, or non-dungeon entities.
- Preserve current health ratio on reapplication; fill health only on initial spawn scaling.
- Base health multiplier is capped at `25.0x` after floor 100.

---

### Task 1: Pure Health Curve

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobScaling.java`
- Create: `src/test/java/tong/statmod/dungeon/DungeonMobScalingTest.java`

**Interfaces:**
- Produces: `static double healthMultiplier(int floor, MobRole role)`
- Produces: `enum MobRole { NORMAL, ELITE, BOSS }`

- [ ] **Step 1: Write failing anchor and interpolation tests**

Test floors `1, 10, 25, 50, 75, 100, 125, 500`, midpoint interpolation, floor clamping, and role multipliers.

- [ ] **Step 2: Verify the tests fail**

Run: `.\gradlew.bat test --tests tong.statmod.dungeon.DungeonMobScalingTest`

Expected: compilation failure because `MobRole` and `healthMultiplier` do not exist.

- [ ] **Step 3: Implement the deterministic curve**

Use linear interpolation between `{1:3, 10:4, 25:6, 50:9, 75:12, 100:15}`. Above 100 use `min(25, 15 + (floor - 100) * 0.10)`, then multiply by `1.25` for elite or `1.5` for boss.

- [ ] **Step 4: Verify the focused tests pass**

Run: `.\gradlew.bat test --tests tong.statmod.dungeon.DungeonMobScalingTest`

Expected: all curve tests pass.

### Task 2: Runtime Application and Roles

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobScaling.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java`
- Modify: boss spawn path found through `DungeonBossTracker` usages.
- Test: `src/test/java/tong/statmod/dungeon/DungeonMobScalingTest.java`

**Interfaces:**
- Consumes: `healthMultiplier(int, MobRole)`
- Produces: stable persistent role tag and non-stacking health modifier application.

- [ ] **Step 1: Write failing tests for modifier amount and role decoding**

Assert that a 3x final multiplier maps to an `ADD_MULTIPLIED_TOTAL` amount of `2.0`, and unknown role data maps to `NORMAL`.

- [ ] **Step 2: Verify tests fail**

Run: `.\gradlew.bat test --tests tong.statmod.dungeon.DungeonMobScalingTest`

- [ ] **Step 3: Apply the stable modifier after L2**

Replace the existing health modifier by ID, calculate `multiplier - 1.0`, preserve health ratio on reapplication, and fill only entities without the applied marker.

- [ ] **Step 4: Tag mini-boss and boss queue entries**

Carry role through the pending spawn record and write it to persistent data before `applyFloorScaling` runs.

- [ ] **Step 5: Run focused and full verification**

Run: `.\gradlew.bat test --tests tong.statmod.dungeon.DungeonMobScalingTest`

Run: `.\gradlew.bat test`

Expected: all tests pass.

### Task 3: Build and Deploy

**Files:**
- Output: `build/libs/statmod-1.2.0.jar`
- Deploy: `C:/Users/El Hadji/AppData/Roaming/.minecraft/versions/test/mods/statmod-1.2.0.jar`

**Interfaces:**
- Consumes: verified Gradle project.
- Produces: deployed client JAR.

- [ ] **Step 1: Build**

Run: `.\gradlew.bat build`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Deploy and hash-check**

Copy the built JAR over the client JAR and compare source/destination SHA-256 hashes.

- [ ] **Step 3: Report exact curve and verification results**

Report tests, build, deployed path, and matching SHA-256.
