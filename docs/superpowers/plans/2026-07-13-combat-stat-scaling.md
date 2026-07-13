# Stronger Combat Stat Scaling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Raise primary weapon-stat damage to `x10` at level 100, strengthen role-specific defenses, and increase deep Trial Dungeon mob health without restoring a dungeon-only melee multiplier.

**Architecture:** Keep all pure combat formulas in `StatCombatScaling` and all deterministic dungeon health anchors in `DungeonMobScaling`. Configuration supplies the offensive curve defaults, while `StatEffectApplier` continues to be the single runtime consumer; the client stat screen must display the same defaults as the server.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1, Gradle 8.10.2, JUnit 5

## Global Constraints

- Primary `BRUTE_FORCE`, `BLADE_TECHNIQUE`, and `PRECISION` weapon scaling reaches exactly `x10` at level 100 with default configuration.
- Secondary `RAPIDITE` and `ARCANE_POWER` weapon scaling keeps weight `0.6` and reaches `x6.6` at level 100.
- Level 100 defensive targets are 65% Physical Resistance, 35% Physical Endurance, 65% Magic Resistance, and 45% Willpower status reduction.
- Physical Resistance and Physical Endurance remain multiplicative, producing 77.25% combined reduction at level 100.
- Dungeon normal-mob health anchors are `x3`, `x4.5`, `x7.5`, `x12`, `x17`, and `x22` at floors 1, 10, 25, 50, 75, and 100.
- Abyss normal-mob health caps at `x32`; elite and boss role multipliers remain `x1.25` and `x1.5`.
- Do not change spell formulas, dungeon mob attack damage, armor, toughness, perks, races, material bonuses, or Epic Fight stamina behavior.
- Do not restore `DungeonMeleeBoost` or `trial_dungeon.meleeDamageMultiplier`.
- Preserve unrelated dirty-worktree changes, especially organic dungeon generation work.

---

### Task 1: Offensive Curve And Shared Defaults

**Files:**
- Modify: `src/main/java/tong/statmod/config/Config.java`
- Modify: `src/main/java/tong/statmod/client/StatTabScreen.java`
- Modify: `src/main/java/tong/statmod/stats/StatCombatScaling.java`
- Modify: `src/test/java/tong/statmod/stats/StatCombatScalingTest.java`

**Interfaces:**
- Produces: `Config.DEFAULT_WEAPON_DAMAGE_BASE` as `double`, value `1.5d`
- Produces: `Config.DEFAULT_WEAPON_DAMAGE_SCALE` as `double`, value `8.5d`
- Preserves: `StatCombatScaling.weaponDamageMultiplier(StatType, int, int, int, int, int, float, float)`
- Consumed by: `StatEffectApplier` through existing `Config.getWeaponDamageBase()` and `Config.getWeaponDamageScale()` calls

- [ ] **Step 1: Replace offensive test expectations with the approved curve**

In `StatCombatScalingTest`, make the primary, secondary, and intermediate-point expectations explicit:

```java
private static final float DEFAULT_BASE = 1.5f;
private static final float DEFAULT_SCALE = 8.5f;

@Test
void primaryWeaponStatsReachTenAtLevelOneHundred() {
    assertEquals(10.0f, StatCombatScaling.weaponDamageMultiplier(
            StatType.BRUTE_FORCE, 100, 0, 0, 0, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
    assertEquals(10.0f, StatCombatScaling.weaponDamageMultiplier(
            StatType.BLADE_TECHNIQUE, 0, 100, 0, 0, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
    assertEquals(10.0f, StatCombatScaling.weaponDamageMultiplier(
            StatType.PRECISION, 0, 0, 100, 0, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
}

@Test
void primaryCurveAcceleratesThroughApprovedMilestones() {
    assertEquals(2.5625f, primaryDamageAt(25), EPSILON);
    assertEquals(4.5052037f, primaryDamageAt(50), EPSILON);
    assertEquals(7.020912f, primaryDamageAt(75), EPSILON);
}

@Test
void fastAndArcaneWeaponsKeepReducedRawDamageWeight() {
    assertEquals(6.6f, StatCombatScaling.weaponDamageMultiplier(
            StatType.RAPIDITE, 0, 0, 0, 100, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
    assertEquals(6.6f, StatCombatScaling.weaponDamageMultiplier(
            StatType.ARCANE_POWER, 0, 0, 0, 0, 100, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
}

private static float primaryDamageAt(int level) {
    return StatCombatScaling.weaponDamageMultiplier(
            StatType.BRUTE_FORCE, level, 0, 0, 0, 0, DEFAULT_BASE, DEFAULT_SCALE);
}
```

- [ ] **Step 2: Run the offensive tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.stats.StatCombatScalingTest
```

Expected: FAIL because the existing test/default curve still expects `scale = 3.0` and primary level 100 produces `x4.5`.

- [ ] **Step 3: Introduce shared configuration defaults**

In `Config`, add constants and use them in both config definitions and fallbacks:

```java
public static final double DEFAULT_WEAPON_DAMAGE_BASE = 1.5d;
public static final double DEFAULT_WEAPON_DAMAGE_SCALE = 8.5d;
```

```java
.defineInRange("weaponDamageBase", DEFAULT_WEAPON_DAMAGE_BASE, 0.5, 10.0);
.defineInRange("weaponDamageScale", DEFAULT_WEAPON_DAMAGE_SCALE, 0.0, 20.0);
```

```java
return DEFAULT_WEAPON_DAMAGE_BASE;
return DEFAULT_WEAPON_DAMAGE_SCALE;
```

Update the `StatCombatScaling` Javadoc to document `scale = 8.5`, primary `x10`, and secondary `x6.6`. Do not alter the formula or weights.

- [ ] **Step 4: Synchronize the client display with the server defaults**

Replace every `1.5f, 3.0f` pair passed to `weaponDamageMultiplier` in `StatTabScreen` with:

```java
(float) Config.DEFAULT_WEAPON_DAMAGE_BASE,
(float) Config.DEFAULT_WEAPON_DAMAGE_SCALE
```

Add `import tong.statmod.config.Config;` if it is not already present.

- [ ] **Step 5: Run the offensive tests and compile the client code**

Run:

```powershell
.\gradlew.bat classes test --tests tong.statmod.stats.StatCombatScalingTest
```

Expected: BUILD SUCCESSFUL, with primary `x10`, secondary `x6.6`, and all intermediate milestones passing.

- [ ] **Step 6: Commit the offensive curve**

```powershell
git add -- src/main/java/tong/statmod/config/Config.java src/main/java/tong/statmod/client/StatTabScreen.java src/main/java/tong/statmod/stats/StatCombatScaling.java src/test/java/tong/statmod/stats/StatCombatScalingTest.java
git commit -m "balance: strengthen offensive combat stat scaling"
```

---

### Task 2: Defensive Curves And Status Duration

**Files:**
- Modify: `src/main/java/tong/statmod/stats/StatCombatScaling.java`
- Modify: `src/test/java/tong/statmod/stats/StatCombatScalingTest.java`

**Interfaces:**
- Preserves: all public method signatures in `StatCombatScaling`
- Produces: level-100 damage-taken multipliers `0.35`, `0.65`, `0.35`, and `0.55` for physical resistance, endurance, magic resistance, and status resistance respectively
- Consumed by: existing `StatEffectApplier.onLivingDamage` and `StatEffectApplier.onMobEffectAdded`

- [ ] **Step 1: Write failing defensive curve tests**

Replace cap assertions based on level 200 with level-100 behavior and explicit over-cap checks:

```java
@Test
void defensiveStatsReachApprovedLevelOneHundredValues() {
    assertEquals(0.35f, StatCombatScaling.physicalDamageTakenMultiplier(100), EPSILON);
    assertEquals(0.35f, StatCombatScaling.magicDamageTakenMultiplier(100), EPSILON);
    assertEquals(0.65f, StatCombatScaling.enduranceDamageTakenMultiplier(100), EPSILON);
    assertEquals(0.55f, StatCombatScaling.statusDamageTakenMultiplier(100), EPSILON);
}

@Test
void defensiveStatsRemainClampedAboveLevelOneHundred() {
    assertEquals(0.35f, StatCombatScaling.physicalDamageTakenMultiplier(200), EPSILON);
    assertEquals(0.35f, StatCombatScaling.magicDamageTakenMultiplier(200), EPSILON);
    assertEquals(0.65f, StatCombatScaling.enduranceDamageTakenMultiplier(200), EPSILON);
    assertEquals(0.55f, StatCombatScaling.statusDamageTakenMultiplier(200), EPSILON);
}

@Test
void levelOneHundredPhysicalDefensesCombineMultiplicatively() {
    assertEquals(0.2275f, StatCombatScaling.incomingDamageMultiplier(
            StatCombatScaling.IncomingDamageRole.PHYSICAL, 100, 100, 100, 100), EPSILON);
}

@Test
void willpowerReducesNegativeEffectDurationWithBoundedPerkBonus() {
    assertEquals(1100, StatCombatScaling.negativeEffectDurationTicks(2000, 100, false));
    assertEquals(900, StatCombatScaling.negativeEffectDurationTicks(2000, 100, true));
    assertEquals(700, StatCombatScaling.negativeEffectDurationTicks(2000, 500, true));
}
```

- [ ] **Step 2: Run the defensive tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.stats.StatCombatScalingTest
```

Expected: FAIL because current level-100 physical/endurance combined damage taken is `0.40`, magic damage taken is `0.50`, and unperked negative effects retain 70% duration.

- [ ] **Step 3: Implement the approved defensive coefficients**

Update only the pure formulas:

```java
public static float physicalDamageTakenMultiplier(int physicalResistance) {
    return 1.0f - Math.min(0.65f, Math.max(0, physicalResistance) * 0.0065f);
}

public static float magicDamageTakenMultiplier(int magicResistance) {
    return 1.0f - Math.min(0.65f, Math.max(0, magicResistance) * 0.0065f);
}

public static float enduranceDamageTakenMultiplier(int physicalEndurance) {
    return 1.0f - Math.min(0.35f, Math.max(0, physicalEndurance) * 0.0035f);
}

public static float statusDamageTakenMultiplier(int willpower) {
    return 1.0f - Math.min(0.45f, Math.max(0, willpower) * 0.0045f);
}
```

Update negative-effect duration reduction:

```java
float reduction = Math.min(0.45f, Math.max(0, willpower) * 0.0045f);
if (ironWillPerkUnlocked) {
    reduction += 0.10f;
}
reduction = Math.min(0.65f, reduction);
```

- [ ] **Step 4: Run defensive tests and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.stats.StatCombatScalingTest
```

Expected: BUILD SUCCESSFUL with the new role-specific caps and duration values.

- [ ] **Step 5: Commit defensive scaling**

```powershell
git add -- src/main/java/tong/statmod/stats/StatCombatScaling.java src/test/java/tong/statmod/stats/StatCombatScalingTest.java
git commit -m "balance: strengthen defensive combat stat scaling"
```

---

### Task 3: Trial Dungeon Health Anchors

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobScaling.java`
- Modify: `src/test/java/tong/statmod/dungeon/DungeonMobScalingTest.java`

**Interfaces:**
- Preserves: `DungeonMobScaling.healthMultiplier(int, MobRole)`
- Preserves: role multipliers `NORMAL = 1.0`, `ELITE = 1.25`, `BOSS = 1.5`
- Produces: updated normal-mob floor curve and Abyss cap
- Consumed by: existing `DungeonMobScaling.applyFloorScaling(LivingEntity, int)`

- [ ] **Step 1: Update health-anchor tests**

Use exact approved anchors and derived role/Abyss values:

```java
@Test
void followsConfiguredHealthAnchors() {
    assertMultiplier(3.0, 1, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(4.5, 10, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(7.5, 25, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(12.0, 50, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(17.0, 75, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(22.0, 100, DungeonMobScaling.MobRole.NORMAL);
}

@Test
void interpolatesAndClampsTheAbyssCurve() {
    assertMultiplier(3.0 + (4.0 / 9.0) * 1.5, 5, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(24.5, 125, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(32.0, 500, DungeonMobScaling.MobRole.NORMAL);
    assertMultiplier(3.0, 0, DungeonMobScaling.MobRole.NORMAL);
}

@Test
void appliesEliteAndBossRoleBonusesAfterTheFloorCurve() {
    assertMultiplier(9.375, 25, DungeonMobScaling.MobRole.ELITE);
    assertMultiplier(11.25, 25, DungeonMobScaling.MobRole.BOSS);
    assertMultiplier(48.0, 500, DungeonMobScaling.MobRole.BOSS);
}
```

- [ ] **Step 2: Run dungeon scaling tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.dungeon.DungeonMobScalingTest
```

Expected: FAIL because the existing floor-100 anchor is `x15`, floor 125 is `x17.5`, and the Abyss cap is `x25`.

- [ ] **Step 3: Implement new anchors and Abyss cap**

In `healthMultiplier`, replace only the normal health curve:

```java
if (safeFloor > 100) {
    base = Math.min(32.0, 22.0 + (safeFloor - 100) * 0.10);
} else {
    int[] floors = {1, 10, 25, 50, 75, 100};
    double[] multipliers = {3.0, 4.5, 7.5, 12.0, 17.0, 22.0};
    // Preserve the existing deterministic linear interpolation loop.
}
```

Update class Javadoc to list the anchor curve rather than the obsolete `+8%/floor` statement. Do not alter attack, armor, toughness, role, or health-ratio reapplication logic.

- [ ] **Step 4: Run dungeon scaling tests and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.dungeon.DungeonMobScalingTest
```

Expected: BUILD SUCCESSFUL with all anchors, interpolation, roles, and cap passing.

- [ ] **Step 5: Commit dungeon health scaling**

```powershell
git add -- src/main/java/tong/statmod/dungeon/DungeonMobScaling.java src/test/java/tong/statmod/dungeon/DungeonMobScalingTest.java
git commit -m "balance: raise deep dungeon mob health anchors"
```

---

### Task 4: Removed Boost Regression And Full Verification

**Files:**
- Modify: `src/main/java/tong/statmod/config/Config.java`
- Delete: `src/main/java/tong/statmod/dungeon/DungeonMeleeBoost.java`
- Modify: `src/test/java/tong/statmod/dungeon/DungeonMeleeBoostTest.java`
- Verify: all files changed by Tasks 1-3

**Interfaces:**
- Produces: no standalone dungeon melee damage subscriber
- Produces: no `DUNGEON_MELEE_DAMAGE_MULTIPLIER`, `meleeDamageMultiplier`, or `getDungeonMeleeDamageMultiplier` production references
- Preserves: normal stat-driven weapon scaling through `StatEffectApplier`

- [ ] **Step 1: Confirm the removal regression test expresses the invariant**

Keep `DungeonMeleeBoostTest` as a source-level invariant:

```java
@Test
void dungeonDoesNotRegisterAStandaloneMeleeDamageBoost() throws IOException {
    assertFalse(Files.exists(Path.of(
            "src/main/java/tong/statmod/dungeon/DungeonMeleeBoost.java")));

    String configSource = Files.readString(Path.of(
            "src/main/java/tong/statmod/config/Config.java"));
    assertFalse(configSource.contains("DUNGEON_MELEE_DAMAGE_MULTIPLIER"));
    assertFalse(configSource.contains("meleeDamageMultiplier"));
    assertFalse(configSource.contains("getDungeonMeleeDamageMultiplier"));
}
```

- [ ] **Step 2: Run focused combat and dungeon tests**

Run:

```powershell
.\gradlew.bat test `
  --tests tong.statmod.stats.StatCombatScalingTest `
  --tests tong.statmod.dungeon.DungeonMobScalingTest `
  --tests tong.statmod.dungeon.DungeonMeleeBoostTest
```

Expected: BUILD SUCCESSFUL, zero failing focused tests.

- [ ] **Step 3: Verify production source contains no removed boost references**

Run:

```powershell
rg -n "DungeonMeleeBoost|getDungeonMeleeDamageMultiplier|DUNGEON_MELEE_DAMAGE_MULTIPLIER|meleeDamageMultiplier" src/main
```

Expected: no matches and ripgrep exit code `1`.

- [ ] **Step 4: Run compilation and the complete test suite**

Run:

```powershell
.\gradlew.bat classes test
```

Expected combat result: all focused combat/dungeon tests pass. If the existing unrelated
`DocumentationConsistencyTest.wikiStatsAndPerksDeclareRuntimeCounts` failure remains, report it
separately and do not change wiki counts as part of this balance patch.

- [ ] **Step 5: Inspect the final scoped diff**

Run:

```powershell
git diff --check
git diff --stat -- `
  src/main/java/tong/statmod/config/Config.java `
  src/main/java/tong/statmod/client/StatTabScreen.java `
  src/main/java/tong/statmod/stats/StatCombatScaling.java `
  src/main/java/tong/statmod/dungeon/DungeonMobScaling.java `
  src/main/java/tong/statmod/dungeon/DungeonMeleeBoost.java `
  src/test/java/tong/statmod/stats/StatCombatScalingTest.java `
  src/test/java/tong/statmod/dungeon/DungeonMobScalingTest.java `
  src/test/java/tong/statmod/dungeon/DungeonMeleeBoostTest.java
```

Expected: only the approved combat balance and prior melee-boost removal appear in the scoped diff.

- [ ] **Step 6: Commit the removal regression if it is still uncommitted**

```powershell
git add -- src/main/java/tong/statmod/config/Config.java src/main/java/tong/statmod/dungeon/DungeonMeleeBoost.java src/test/java/tong/statmod/dungeon/DungeonMeleeBoostTest.java
git commit -m "balance: remove dungeon-only melee damage boost"
```

Do not stage logs, organic dungeon generation files, debug helpers, or unrelated documentation.
