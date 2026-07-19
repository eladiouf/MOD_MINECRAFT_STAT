# Dungeon Enemy Health Halving Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce the final maximum health of every hostile STAT Trial Dungeon enemy by exactly 50 percent without changing neutral inhabitants or any other combat statistic.

**Architecture:** A focused `DungeonEnemyHealthBalance` class owns one stable `MULTIPLY_TOTAL` attribute modifier and ratio-preserving reapplication. Existing spawn/scaling paths call it after their own health calculations; the occupied-floor director reapplies it idempotently to retrofit persisted enemies.

**Tech Stack:** Java 17, Forge 1.20.1-47.4.4 attributes/NBT, JUnit 5, Gradle 8.8.

## Global Constraints

- Apply a final maximum-health multiplier of exactly `0.5`.
- Apply only to hostile entities managed by STAT Mod in `statmod:trial_dungeon`.
- Exclude players and every actor marked `statmod_living_non_combat`.
- Preserve current health percentage during first and repeated applications.
- Do not modify damage, armor, spells, AI, encounter size or neutral inhabitants.
- Preserve unrelated untracked workspace files.

---

### Task 1: Idempotent enemy-health balance component

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonEnemyHealthBalance.java`
- Create: `src/test/java/tong/statmod/dungeon/DungeonEnemyHealthBalanceTest.java`

**Interfaces:**
- Produces: `public static void apply(LivingEntity entity)`.
- Produces package-visible pure helpers `maxHealthMultiplier()`, `modifierAmount()` and `healthAtSameRatio(double, double, double)` for deterministic tests.

- [x] **Step 1: Write the failing unit tests**

```java
package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class DungeonEnemyHealthBalanceTest {
    @Test void finalMaximumHealthIsHalvedExactly() {
        assertEquals(0.5D, DungeonEnemyHealthBalance.maxHealthMultiplier(), 1.0e-9);
        assertEquals(-0.5D, DungeonEnemyHealthBalance.modifierAmount(), 1.0e-9);
    }

    @Test void rebalancingPreservesCurrentHealthRatio() {
        assertEquals(60.0D,
                DungeonEnemyHealthBalance.healthAtSameRatio(200.0D, 120.0D, 100.0D), 1.0e-9);
        assertEquals(60.0D,
                DungeonEnemyHealthBalance.healthAtSameRatio(100.0D, 60.0D, 100.0D), 1.0e-9);
    }
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonEnemyHealthBalanceTest" --console=plain`

Expected: compilation failure because `DungeonEnemyHealthBalance` does not exist.

- [x] **Step 3: Implement the minimal component**

Create a final utility class containing:

```java
private static final double MAX_HEALTH_MULTIPLIER = 0.5D;
private static final ResourceLocation MODIFIER_ID =
        new ResourceLocation(StatMod.MOD_ID, "dungeon_enemy_health_balance");

static double maxHealthMultiplier() { return MAX_HEALTH_MULTIPLIER; }
static double modifierAmount() { return MAX_HEALTH_MULTIPLIER - 1.0D; }

static double healthAtSameRatio(double oldMax, double oldHealth, double newMax) {
    double ratio = oldMax > 0.0D ? oldHealth / oldMax : 1.0D;
    return Math.max(0.0D, Math.min(newMax, newMax * ratio));
}
```

`apply` must return for null or `DungeonLivingActor.isNonCombat(entity)` when the entity is a `Mob`; remove the stable UUID modifier, add one transient `MULTIPLY_TOTAL` modifier with amount `-0.5`, then restore health through `healthAtSameRatio`.

- [x] **Step 4: Run the focused test and verify GREEN**

Run: `.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonEnemyHealthBalanceTest" --console=plain`

Expected: 2 tests pass.

- [x] **Step 5: Commit**

```powershell
git add -- src/main/java/tong/statmod/dungeon/DungeonEnemyHealthBalance.java src/test/java/tong/statmod/dungeon/DungeonEnemyHealthBalanceTest.java
git commit -m "feat: halve hostile dungeon mob health"
```

### Task 2: Cover every hostile spawn and recovery path

**Files:**
- Create: `src/test/java/tong/statmod/dungeon/DungeonEnemyHealthWiringContractTest.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobScaling.java`
- Modify: `src/main/java/tong/statmod/dungeon/party/AdventurerPartyHelper.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonTrapHandler.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonSecretRoom.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonUltraVault.java`
- Modify: `src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java`

**Interfaces:**
- Consumes: `DungeonEnemyHealthBalance.apply(LivingEntity)` from Task 1.
- Produces: seven independently verified integration calls covering scaling, parties, altars, trap ambushes, hidden bosses and persisted actors.

- [x] **Step 1: Write the failing wiring contract**

The test reads the seven production sources and asserts each contains `DungeonEnemyHealthBalance.apply`. It also asserts the director applies the balance only inside its existing `!DungeonLivingActor.isNonCombat(actor)` branch.

- [x] **Step 2: Run the focused wiring test and verify RED**

Run: `.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonEnemyHealthWiringContractTest" --console=plain`

Expected: assertions fail because no integration calls exist.

- [x] **Step 3: Add the seven minimal calls**

- At the end of `DungeonMobScaling.applyFloorScaling`, call `DungeonEnemyHealthBalance.apply(mob)`.
- At the end of `AdventurerPartyHelper.configureRole`, call `DungeonEnemyHealthBalance.apply(entity)`.
- In `DungeonBossAltarBlock`, call the component for every spawned `LivingEntity` after boss setup.
- Apply it to each `LivingEntity` created by `DungeonTrapHandler`, `DungeonSecretRoom` and `DungeonUltraVault` before combat begins.
- In `DungeonEncounterDirector`, apply it beside `DungeonTacticalGoals.ensureAttached(actor)` only for non-combat actors.

- [x] **Step 4: Run both focused test classes and verify GREEN**

Run: `.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonEnemyHealth*" --console=plain`

Expected: all health-balance unit and wiring tests pass.

- [x] **Step 5: Commit**

```powershell
git add -- src/main/java/tong/statmod/dungeon/DungeonMobScaling.java src/main/java/tong/statmod/dungeon/party/AdventurerPartyHelper.java src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java src/main/java/tong/statmod/dungeon/DungeonTrapHandler.java src/main/java/tong/statmod/dungeon/DungeonSecretRoom.java src/main/java/tong/statmod/dungeon/DungeonUltraVault.java src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java src/test/java/tong/statmod/dungeon/DungeonEnemyHealthWiringContractTest.java
git commit -m "feat: apply health reduction to every dungeon enemy"
```

### Task 3: Full verification, deployment and publication

**Files:**
- Modify: `docs/forge-1.20.1-server-validation.md`
- Modify: `docs/superpowers/plans/2026-07-19-dungeon-enemy-health-halving.md`

**Interfaces:**
- Consumes: verified build artifact `build/libs/statmod-0.1.0+1.20.1.jar`.
- Produces: byte-identical client/server deployment and rollback copies.

- [x] **Step 1: Run the complete release gate**

Run: `.\gradlew.bat clean test build --console=plain`

Expected: build successful, zero test failures.

- [x] **Step 2: Verify artifact and transactional targets**

Record the JAR size and SHA-256. Confirm no Minecraft client or Forge server process is active and both targets contain all mandatory dependencies.

- [x] **Step 3: Back up and deploy only Stat Mod**

Create timestamped `statmod-backups/health-halving-<timestamp>` directories under the active client and server roots, copy the previous Stat Mod JARs, and replace them with the verified artifact. Require exactly one Stat Mod JAR and matching SHA-256 on build, client and server.

- [x] **Step 4: Update validation evidence**

Append test count, artifact hash, deployment targets and rollback paths to `docs/forge-1.20.1-server-validation.md`. Mark this plan complete.

- [x] **Step 5: Commit and push**

```powershell
git add -- docs/forge-1.20.1-server-validation.md docs/superpowers/plans/2026-07-19-dungeon-enemy-health-halving.md
git commit -m "docs: validate dungeon enemy health balance"
git push origin forge-1.20.1
```

Verify `HEAD` equals `origin/forge-1.20.1` and preserve unrelated untracked files.
