# Adventurer Healer Full Nerf Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make rival adventurer parties beatable by reducing the healer's cadence, healing output, emergency recovery, defensive uptime and self-preservation.

**Architecture:** Keep `HealPartyGoal` and its current action flow intact. Extend the existing source contract to lock every validated value and removal, then make only the corresponding constant and effect changes in the goal.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, JUnit 5, Gradle 8.8

## Global Constraints

- Normal heal cooldown is 200 ticks; amount is `5.0 + max health * 0.05`; splash is 25%; no normal-heal Regeneration.
- Sanctuary activates for two allies below 30%, restores 20%, has a 600-tick cooldown, and grants Regeneration I for 40 ticks plus Resistance I for 120 ticks.
- Shield/cleanse interval is 300 ticks; Absorption I lasts 100 ticks.
- War hymn interval is 300 ticks; Strength I and Speed I last 160 ticks; no hymn Resistance.
- Self-heal priority begins below 60%; ally eligibility begins below 75%.
- No other adventurer role or dungeon subsystem changes.

---

### Task 1: Lock the complete healer nerf with a failing contract

**Files:**
- Modify: `src/test/java/tong/statmod/dungeon/party/goal/HealPartyGoalRegenerationContractTest.java`

**Interfaces:**
- Consumes: the source of `HealPartyGoal`.
- Produces: an exact regression contract for cooldowns, amounts, thresholds, effects and removed effects.

- [ ] **Step 1: Replace the focused regeneration test with the complete contract**

Keep the existing source loading and assert all of the following exact fragments and occurrence counts:

```java
assertTrue(source.contains("HEAL_COOLDOWN = 200"));
assertTrue(source.contains("BUFF_INTERVAL = 300"));
assertTrue(source.contains("SHIELD_INTERVAL = 300"));
assertTrue(source.contains("5.0f + healer.getMaxHealth() * 0.05f"));
assertTrue(source.contains("ally.heal(amount * 0.25f)"));
assertTrue(source.contains("ultCooldown = 600"));
assertTrue(source.contains("healer.getMaxHealth() * 0.30"));
assertTrue(source.contains("ally.getMaxHealth() * 0.30"));
assertTrue(source.contains("ally.heal(ally.getMaxHealth() * 0.2f)"));
assertTrue(source.contains("MobEffects.REGENERATION, 40, 0, false, true"));
assertEquals(1, source.split("MobEffects.REGENERATION", -1).length - 1);
assertTrue(source.contains("MobEffects.ABSORPTION, 100, 0, false, true"));
assertTrue(source.contains("MobEffects.DAMAGE_BOOST, 160, 0, false, true"));
assertTrue(source.contains("MobEffects.MOVEMENT_SPEED, 160, 0, false, true"));
assertTrue(source.contains("MobEffects.DAMAGE_RESISTANCE, 120, 0, false, true"));
assertEquals(1, source.split("MobEffects.DAMAGE_RESISTANCE", -1).length - 1);
assertTrue(source.contains("healer.getMaxHealth() * 0.60"));
assertTrue(source.contains("float bestFrac = 0.75f"));
```

- [ ] **Step 2: Run the focused test and verify RED**

```powershell
& './gradlew.bat' test --tests tong.statmod.dungeon.party.goal.HealPartyGoalRegenerationContractTest --console=plain
```

Expected: FAIL because the current goal still uses the pre-nerf cooldowns, amounts, thresholds and effects.

### Task 2: Apply the validated healer values

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/party/goal/HealPartyGoal.java`

**Interfaces:**
- Consumes: the exact contract from Task 1.
- Produces: the validated combat behavior without changing control flow or public APIs.

- [ ] **Step 1: Change the three timing constants**

Set `HEAL_COOLDOWN`, `BUFF_INTERVAL` and `SHIELD_INTERVAL` to `200`, `300` and `300`.

- [ ] **Step 2: Reduce normal healing**

Use `5.0f + healer.getMaxHealth() * 0.05f`, remove its Regeneration application, and change nearby splash healing to `amount * 0.25f`.

- [ ] **Step 3: Reduce hymn and shielding**

Give Strength I and Speed I for 160 ticks, remove hymn Resistance, and change absorption to `MobEffects.ABSORPTION, 100, 0, false, true`.

- [ ] **Step 4: Reduce patient selection and Sanctuary**

Set self-priority to `0.60`, ally threshold to `0.75f`, both critical checks to `0.30`, Sanctuary healing to `0.2f`, Sanctuary Regeneration to 40 ticks, and its cooldown to 600 ticks. Keep Sanctuary Resistance at 120 ticks.

- [ ] **Step 5: Verify focused and complete tests**

```powershell
& './gradlew.bat' test --tests tong.statmod.dungeon.party.goal.HealPartyGoalRegenerationContractTest --console=plain
& './gradlew.bat' clean test build --console=plain
```

Expected: focused PASS, full `BUILD SUCCESSFUL`, zero failures and zero errors.

- [ ] **Step 6: Commit behavior and plan progress**

```powershell
git add src/main/java/tong/statmod/dungeon/party/goal/HealPartyGoal.java src/test/java/tong/statmod/dungeon/party/goal/HealPartyGoalRegenerationContractTest.java docs/superpowers/plans/2026-07-19-adventurer-healer-full-nerf.md
git commit -m "balance: make adventurer healers beatable"
```

### Task 3: Deploy and publish

**Files:**
- Modify: `docs/forge-1.20.1-server-validation.md`
- Deploy: client and dedicated-server Stat Mod JAR paths already recorded in the validation document.

**Interfaces:**
- Consumes: `build/libs/statmod-0.1.0+1.20.1.jar`.
- Produces: matching client/server artifacts, Java 17 smoke evidence and a published branch.

- [ ] **Step 1: Back up both deployed JARs and replace them with the validated artifact**

Use timestamped `statmod-backups/healer-full-nerf-*` directories beside the client and server installations before copying the artifact.

- [ ] **Step 2: Verify deployment identity**

Run SHA-256 on the build, client and server JARs and require exactly one unique hash.

- [ ] **Step 3: Run the Java 17 dedicated-server gate**

Start the server with `C:/Program Files/Java/jdk-17/bin/java.exe`, wait for `Done (`, send one empty console line followed by `stop`, then require `Stopping server`, `All dimensions are saved`, exit code 0 and zero remaining Forge server processes.

- [ ] **Step 4: Document exact evidence**

Append the validated balance, test totals, artifact size/hash, deployment paths, backup paths and smoke markers to `docs/forge-1.20.1-server-validation.md`.

- [ ] **Step 5: Commit, push and verify publication**

Commit the validation documentation, push `forge-1.20.1`, fetch the remote ref, require local/remote equality, and preserve the existing worktree.
