# Forge Physical Endurance Stamina Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scale Epic Fight and ParCool stamina capacity/recovery from STAT Mod Physical Endurance without introducing optional compile-time dependencies or another stamina pool.

**Architecture:** A pure scaling function computes bounded `MULTIPLY_BASE` amounts. A server adapter resolves four optional attributes by registry ID and idempotently replaces stable transient modifiers during player/stat lifecycle refreshes.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.x, ForgeGradle 6, JUnit 5.

## Global Constraints

- Target Minecraft 1.20.1, Forge 47.x, and Java 17.
- Do not import Epic Fight or ParCool classes and do not add either mod to Gradle or `mods.toml`.
- Do not create, copy, refill, or synchronize current stamina values.
- Use exactly `epicfight:staminar`, `epicfight:stamina_regen`, `parcool:max_stamina`, and `parcool:stamina_recovery`.
- Use transient `MULTIPLY_BASE` modifiers with a different stable UUID for every target.
- Level 0 is neutral; defaults at level 100 are +100% capacity and +50% recovery.
- Missing optional attributes and missing player attribute instances are normal no-op cases.
- Preserve unrelated modpack script changes already present in the worktree.

---

### Task 1: Pure Physical Endurance scaling

**Files:**
- Create: `src/main/java/tong/statmod/effects/PhysicalEnduranceScaling.java`
- Create: `src/test/java/tong/statmod/effects/PhysicalEnduranceScalingTest.java`

**Interfaces:**
- Produces: `public static double bonus(int level, double bonusAt100)`.

- [ ] **Step 1: Write the failing unit test**

```java
package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PhysicalEnduranceScalingTest {
    @Test void isNeutralAtZero() { assertEquals(0.0, PhysicalEnduranceScaling.bonus(0, 1.0)); }
    @Test void reachesConfiguredBonusAtOneHundred() { assertEquals(1.0, PhysicalEnduranceScaling.bonus(100, 1.0)); }
    @Test void scalesLinearly() { assertEquals(0.25, PhysicalEnduranceScaling.bonus(50, 0.5)); }
    @Test void clampsLevel() {
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(-1, 1.0));
        assertEquals(1.0, PhysicalEnduranceScaling.bonus(101, 1.0));
    }
    @Test void rejectsUnsafeConfiguredAmounts() {
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(100, -1.0));
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(100, Double.NaN));
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(100, Double.POSITIVE_INFINITY));
    }
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.effects.PhysicalEnduranceScalingTest`

Expected: compilation failure because `PhysicalEnduranceScaling` does not exist.

- [ ] **Step 3: Implement the pure function**

```java
package tong.statmod.effects;

public final class PhysicalEnduranceScaling {
    private PhysicalEnduranceScaling() {}

    public static double bonus(int level, double bonusAt100) {
        if (!Double.isFinite(bonusAt100) || bonusAt100 <= 0.0) return 0.0;
        int safeLevel = Math.max(0, Math.min(100, level));
        return bonusAt100 * safeLevel / 100.0;
    }
}
```

- [ ] **Step 4: Run focused and full unit tests**

Run: `.\gradlew.bat test --tests tong.statmod.effects.PhysicalEnduranceScalingTest`

Expected: PASS, five tests.

Run: `.\gradlew.bat test`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/effects/PhysicalEnduranceScaling.java src/test/java/tong/statmod/effects/PhysicalEnduranceScalingTest.java
git commit -m "feat: define endurance stamina scaling"
```

### Task 2: Server configuration and optional target definitions

**Files:**
- Modify: `src/main/java/tong/statmod/config/StatModServerConfig.java`
- Create: `src/main/java/tong/statmod/effects/StaminaAttributeTarget.java`
- Create: `src/test/java/tong/statmod/effects/StaminaAttributeTargetTest.java`
- Modify: `src/test/java/tong/statmod/config/StatModServerConfigContractTest.java`

**Interfaces:**
- Consumes: `PhysicalEnduranceScaling.bonus(int, double)`.
- Produces: `StatModServerConfig.staminaCapacityBonusAt100()`, `staminaRecoveryBonusAt100()`, and `StaminaAttributeTarget.values()`.

- [ ] **Step 1: Write failing target and config contract tests**

Create tests asserting that the four target IDs exactly equal the Global Constraints, all four UUIDs are unique, two targets use `BonusKind.CAPACITY`, two use `BonusKind.RECOVERY`, and the config source contains an `endurance` section with defaults `1.0` and `0.5` in range `0.0..5.0`.

```java
@Test void exposesExactOptionalRegistryIds() {
    assertEquals(Set.of("epicfight:staminar", "epicfight:stamina_regen",
            "parcool:max_stamina", "parcool:stamina_recovery"),
            Arrays.stream(StaminaAttributeTarget.values())
                    .map(target -> target.id().toString()).collect(Collectors.toSet()));
}

@Test void givesEveryTargetAUniqueStableUuid() {
    assertEquals(4, Arrays.stream(StaminaAttributeTarget.values())
            .map(StaminaAttributeTarget::modifierId).distinct().count());
}
```

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.effects.StaminaAttributeTargetTest --tests tong.statmod.config.StatModServerConfigContractTest`

Expected: failure because targets and endurance config accessors do not exist.

- [ ] **Step 3: Implement target definitions and config accessors**

Define `StaminaAttributeTarget` as an enum with four constants, `ResourceLocation id`, `UUID modifierId`, and `BonusKind bonusKind`. Use literal UUID strings committed to source and the modifier name `STAT Mod Physical Endurance`. Add an `endurance` config section and public double accessors returning each `DoubleValue`.

```java
public enum BonusKind { CAPACITY, RECOVERY }

public double amount(int level) {
    double maximum = bonusKind == BonusKind.CAPACITY
            ? StatModServerConfig.staminaCapacityBonusAt100()
            : StatModServerConfig.staminaRecoveryBonusAt100();
    return PhysicalEnduranceScaling.bonus(level, maximum);
}
```

- [ ] **Step 4: Run focused and full unit tests**

Run both focused tests, then `.\gradlew.bat test`.

Expected: BUILD SUCCESSFUL for both commands.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/config/StatModServerConfig.java src/main/java/tong/statmod/effects/StaminaAttributeTarget.java src/test/java/tong/statmod/effects/StaminaAttributeTargetTest.java src/test/java/tong/statmod/config/StatModServerConfigContractTest.java
git commit -m "feat: configure optional stamina targets"
```

### Task 3: Idempotent player attribute adapter

**Files:**
- Create: `src/main/java/tong/statmod/effects/PlayerAttributeEffects.java`
- Create: `src/test/java/tong/statmod/effects/PlayerAttributeEffectsContractTest.java`

**Interfaces:**
- Consumes: `StaminaAttributeTarget.values()` and player `StatCapabilities.PLAYER_STATS`.
- Produces: `public static void refresh(ServerPlayer player)`.

- [ ] **Step 1: Write the failing source contract test**

Read the production source and assert it contains `ForgeRegistries.ATTRIBUTES.getValue`, `removeModifier`, `addTransientModifier`, `AttributeModifier.Operation.MULTIPLY_BASE`, and `StatType.PHYSICAL_ENDURANCE`. Assert it does not contain imports whose package begins with `yesman.epicfight` or `com.alrex.parcool`.

- [ ] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.effects.PlayerAttributeEffectsContractTest`

Expected: failure because the adapter source does not exist.

- [ ] **Step 3: Implement the adapter**

```java
public static void refresh(ServerPlayer player) {
    if (player == null) return;
    PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
            .resolve().orElse(null);
    if (stats == null) return;
    int level = stats.get(StatType.PHYSICAL_ENDURANCE).level();
    for (StaminaAttributeTarget target : StaminaAttributeTarget.values()) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(target.id());
        if (attribute == null) continue;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) continue;
        instance.removeModifier(target.modifierId());
        double amount = target.amount(level);
        if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(target.modifierId(),
                    "STAT Mod Physical Endurance", amount,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}
```

- [ ] **Step 4: Run focused and full unit tests**

Run the focused contract test, then `.\gradlew.bat test`.

Expected: BUILD SUCCESSFUL and no optional mod required on the classpath.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/effects/PlayerAttributeEffects.java src/test/java/tong/statmod/effects/PlayerAttributeEffectsContractTest.java
git commit -m "feat: bridge endurance to stamina attributes"
```

### Task 4: Lifecycle wiring and production verification

**Files:**
- Modify: `src/main/java/tong/statmod/event/PlayerStatsEvents.java`
- Modify: `src/main/java/tong/statmod/command/StatsCommands.java`
- Modify: `src/main/java/tong/statmod/progression/xp/XpAwardService.java`
- Create: `src/test/java/tong/statmod/effects/PlayerAttributeEffectsWiringTest.java`
- Modify: `docs/compatibility/stat-attribute-provider-matrix.md`

**Interfaces:**
- Consumes: `PlayerAttributeEffects.refresh(ServerPlayer)`.
- Preserves: existing snapshot networking and XP award return behavior.

- [ ] **Step 1: Write the failing lifecycle source contract**

Assert that `PlayerStatsEvents.sync` refreshes before `sendSnapshot`, command mutation refreshes before its snapshot, and `XpAwardService` captures Physical Endurance before `XpAwardCoordinator.apply` then refreshes only when the post-award level differs.

```java
assertTrue(events.indexOf("PlayerAttributeEffects.refresh(serverPlayer)")
        < events.indexOf("StatNetwork.sendSnapshot(serverPlayer)"));
assertTrue(commands.indexOf("PlayerAttributeEffects.refresh(target)")
        < commands.indexOf("StatNetwork.sendSnapshot(target)"));
assertTrue(xp.contains("beforeEnduranceLevel"));
assertTrue(xp.contains("afterEnduranceLevel != beforeEnduranceLevel"));
```

- [ ] **Step 2: Run the lifecycle test and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.effects.PlayerAttributeEffectsWiringTest`

Expected: assertions fail because refresh calls are absent.

- [ ] **Step 3: Wire lifecycle refreshes minimally**

Call the adapter in `PlayerStatsEvents.sync` and `StatsCommands.mutate` before snapshots. In `XpAwardService.award`, store `stats.get(StatType.PHYSICAL_ENDURANCE).level()` before coordinator application; after a changed result, refresh only if the current level differs, then send the snapshot.

- [ ] **Step 4: Document implemented status**

Update the Physical Endurance row and implementation-order section in `docs/compatibility/stat-attribute-provider-matrix.md` to say the registry-only bridge is implemented, give the default bonuses, and name the lifecycle refresh points.

- [ ] **Step 5: Run complete verification**

Run:

```powershell
.\gradlew.bat clean test build
powershell -ExecutionPolicy Bypass -File scripts\verify-forge-artifact.ps1
.\gradlew.bat runGameTestServer
```

Expected: all tests pass; clean build succeeds; verifier reports every check OK; the finite GameTest dedicated server starts, saves, and exits without a fatal error.

- [ ] **Step 6: Inspect the final diff and commit**

Run `git diff --check` and `git status --short`. Confirm only the five task files plus the known unrelated modpack script changes are present.

```powershell
git add src/main/java/tong/statmod/event/PlayerStatsEvents.java src/main/java/tong/statmod/command/StatsCommands.java src/main/java/tong/statmod/progression/xp/XpAwardService.java src/test/java/tong/statmod/effects/PlayerAttributeEffectsWiringTest.java docs/compatibility/stat-attribute-provider-matrix.md
git commit -m "feat: refresh stamina bridge across player lifecycle"
```

