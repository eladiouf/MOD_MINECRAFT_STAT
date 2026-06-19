# Elementals Mage Class Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate `Elementals` as a `STAT Mod`-driven mage-class path with race-based starters, stat/perk-gated progression, awakened/mastered states, rare `blood` and `lightning` grimoires, and native `Elementals` casting/UI preserved.

**Architecture:** Keep `STAT Mod` as the source of truth for mage awakening, allowed elements, mastery state, penalties, and rare unlocks. Keep `Elementals` as the native runtime layer by reconciling `STAT Mod` decisions into `dev.saperate.elementals.data.Bender` and `dev.saperate.elementals.data.PlayerData` instead of replacing its HUD, keybinds, or upgrade screen.

**Tech Stack:** NeoForge 1.21.1, Java 21, NeoForge attachments, NeoForge events, existing `STAT Mod` perk system, `Elementals` runtime classes (`Bender`, `PlayerData`, `Element`, `Upgrade`), JUnit 5, Gradle

---

## File Structure

### New units

- `src/main/java/tong/statmod/integration/elementals/ElementalBranch.java`
  - canonical branch enum for `AIR`, `WATER`, `EARTH`, `FIRE`, `LIGHTNING`, `BLOOD`
- `src/main/java/tong/statmod/integration/elementals/ElementState.java`
  - canonical state enum for `LOCKED`, `AWAKENED`, `MASTERED`
- `src/main/java/tong/statmod/integration/elementals/MageRaceProfile.java`
  - supported-race descriptor for `human`, `elf`, `dwarf`, `beastfolk`
- `src/main/java/tong/statmod/integration/elementals/ElementalsMageData.java`
  - attachment-backed persistent mage state, starter branches, unlocked branches, and last seen `Elementals` runtime values
- `src/main/java/tong/statmod/integration/elementals/ElementalsRaceAffinity.java`
  - supported-race mapping and deterministic human starter-pair selection
- `src/main/java/tong/statmod/integration/elementals/ElementalsPerkBindings.java`
  - exact existing perk ids required or rewarded by each progression gate
- `src/main/java/tong/statmod/integration/elementals/ElementalsMageRules.java`
  - pure stat-threshold and branch-state rules from the spec
- `src/main/java/tong/statmod/integration/elementals/ElementalsPenaltyModel.java`
  - awakened damage/chi penalties and beastfolk/rare-branch progression multipliers
- `src/main/java/tong/statmod/integration/elementals/ElementalsRuntimePort.java`
  - tiny runtime port used by pure reconciliation tests
- `src/main/java/tong/statmod/integration/elementals/ElementalsRuntimeBridge.java`
  - thin adapter over `Bender`, `PlayerData`, and `Element`
- `src/main/java/tong/statmod/integration/elementals/ElementalsCompat.java`
  - init, player reconciliation, login/tick hooks, and runtime sync
- `src/main/java/tong/statmod/integration/elementals/ElementalsCombatScalingHandler.java`
  - awakened damage penalty for `Elementals` damage sources
- `src/main/java/tong/statmod/item/ElementalGrimoireItem.java`
  - custom `STAT Mod` grimoire for `blood` and `lightning` unlock flow

### Existing files to modify

- `src/main/java/tong/statmod/storage/ModAttachments.java`
  - register and serialize `ElementalsMageData`
- `src/main/java/tong/statmod/STATMod.java`
  - initialize `ElementalsCompat` when the `elementals` mod is loaded
- `src/main/java/tong/statmod/item/ModItems.java`
  - register `blood_grimoire` and `lightning_grimoire`
- `src/main/java/tong/statmod/network/SyncHelper.java`
  - no new payload; reuse `syncStats` and `syncPerks` after mage-state or reward changes
- `src/main/java/tong/statmod/stats/StatEffectApplier.java`
  - do not mix `Elementals` damage logic here; leave damage scaling in dedicated `ElementalsCombatScalingHandler`
- `src/main/resources/assets/statmod/lang/en_us.json`
  - add grimoire names and failure/success feedback keys
- `src/main/resources/assets/statmod/lang/fr_fr.json`
  - add French grimoire names and feedback keys
- `src/main/resources/assets/statmod/models/item/blood_grimoire.json`
  - simple generated model
- `src/main/resources/assets/statmod/models/item/lightning_grimoire.json`
  - simple generated model

### Tests

- `src/test/java/tong/statmod/integration/elementals/ElementalsRaceAffinityTest.java`
- `src/test/java/tong/statmod/integration/elementals/ElementalsMageRulesTest.java`
- `src/test/java/tong/statmod/integration/elementals/ElementalsPerkBindingsTest.java`
- `src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java`
- `src/test/java/tong/statmod/integration/elementals/ElementalsPenaltyModelTest.java`
- `src/test/java/tong/statmod/item/ElementalGrimoireItemTest.java`

---

### Task 1: Add Persistent Mage State and Race Affinity Rules

**Files:**
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalBranch.java`
- Create: `src/main/java/tong/statmod/integration/elementals/ElementState.java`
- Create: `src/main/java/tong/statmod/integration/elementals/MageRaceProfile.java`
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsMageData.java`
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsRaceAffinity.java`
- Modify: `src/main/java/tong/statmod/storage/ModAttachments.java`
- Test: `src/test/java/tong/statmod/integration/elementals/ElementalsRaceAffinityTest.java`

- [ ] **Step 1: Write the failing race-affinity and persistence tests**

```java
package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsRaceAffinityTest {
    @Test
    void supportsOnlyTheFourApprovedMageRaces() {
        assertTrue(ElementalsRaceAffinity.resolve("tensura:elf").supported());
        assertTrue(ElementalsRaceAffinity.resolve("tensura:human").supported());
        assertTrue(ElementalsRaceAffinity.resolve("tensura:dwarf").supported());
        assertTrue(ElementalsRaceAffinity.resolve("tensura:beastfolk").supported());
        assertFalse(ElementalsRaceAffinity.resolve("tensura:slime").supported());
    }

    @Test
    void fixedRacesKeepTheirApprovedStarterPairs() {
        assertEquals(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER),
                ElementalsRaceAffinity.starterBranches(ElementalsRaceAffinity.resolve("tensura:elf"), UUID.fromString("00000000-0000-0000-0000-000000000001")));
        assertEquals(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH),
                ElementalsRaceAffinity.starterBranches(ElementalsRaceAffinity.resolve("tensura:dwarf"), UUID.fromString("00000000-0000-0000-0000-000000000002")));
        assertEquals(EnumSet.of(ElementalBranch.WATER, ElementalBranch.AIR),
                ElementalsRaceAffinity.starterBranches(ElementalsRaceAffinity.resolve("tensura:beastfolk"), UUID.fromString("00000000-0000-0000-0000-000000000003")));
    }

    @Test
    void humansGetDeterministicBaseOnlyStarterPairs() {
        MageRaceProfile profile = ElementalsRaceAffinity.resolve("tensura:human");
        EnumSet<ElementalBranch> starters = ElementalsRaceAffinity.starterBranches(profile, UUID.fromString("11111111-2222-3333-4444-555555555555"));
        assertEquals(2, starters.size());
        assertFalse(starters.contains(ElementalBranch.LIGHTNING));
        assertFalse(starters.contains(ElementalBranch.BLOOD));
    }
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsRaceAffinityTest`  
Expected: FAIL with missing `ElementalsRaceAffinity`, `ElementalBranch`, and `MageRaceProfile`

- [ ] **Step 3: Write the minimal enums, race resolver, mage attachment, and serializer**

```java
package tong.statmod.integration.elementals;

public enum ElementalBranch {
    AIR("air", true),
    WATER("water", true),
    EARTH("earth", true),
    FIRE("fire", true),
    LIGHTNING("lightning", false),
    BLOOD("blood", false);

    private final String elementalsName;
    private final boolean baseBranch;

    ElementalBranch(String elementalsName, boolean baseBranch) {
        this.elementalsName = elementalsName;
        this.baseBranch = baseBranch;
    }

    public String elementalsName() {
        return elementalsName;
    }

    public boolean isBaseBranch() {
        return baseBranch;
    }
}
```

```java
package tong.statmod.integration.elementals;

public enum ElementState {
    LOCKED,
    AWAKENED,
    MASTERED
}
```

```java
package tong.statmod.integration.elementals;

import java.util.EnumSet;

public record MageRaceProfile(
        String raceId,
        boolean supported,
        boolean human,
        boolean beastfolk,
        EnumSet<ElementalBranch> fixedStarters
) {}
```

```java
package tong.statmod.integration.elementals;

import java.util.EnumSet;

public final class ElementalsMageData {
    private boolean mageAwakened;
    private final EnumSet<ElementalBranch> starterBranches = EnumSet.noneOf(ElementalBranch.class);
    private final EnumSet<ElementalBranch> unlockedBranches = EnumSet.noneOf(ElementalBranch.class);
    private final EnumSet<ElementalBranch> rewardedRareBranches = EnumSet.noneOf(ElementalBranch.class);
    private float lastSeenChi;
    private float lastSeenXp;
    private int lastSeenLevel;

    public boolean mageAwakened() { return mageAwakened; }
    public void setMageAwakened(boolean value) { mageAwakened = value; }
    public EnumSet<ElementalBranch> starterBranches() { return EnumSet.copyOf(starterBranches); }
    public EnumSet<ElementalBranch> unlockedBranches() { return EnumSet.copyOf(unlockedBranches); }
    public EnumSet<ElementalBranch> rewardedRareBranches() { return EnumSet.copyOf(rewardedRareBranches); }
    public void setStarterBranches(EnumSet<ElementalBranch> branches) { starterBranches.clear(); starterBranches.addAll(branches); }
    public void setUnlockedBranches(EnumSet<ElementalBranch> branches) { unlockedBranches.clear(); unlockedBranches.addAll(branches); }
    public void setRewardedRareBranches(EnumSet<ElementalBranch> branches) { rewardedRareBranches.clear(); rewardedRareBranches.addAll(branches); }
    public float lastSeenChi() { return lastSeenChi; }
    public void setLastSeenChi(float value) { lastSeenChi = value; }
    public float lastSeenXp() { return lastSeenXp; }
    public void setLastSeenXp(float value) { lastSeenXp = value; }
    public int lastSeenLevel() { return lastSeenLevel; }
    public void setLastSeenLevel(int value) { lastSeenLevel = Math.max(0, value); }
}
```

```java
package tong.statmod.integration.elementals;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class ElementalsRaceAffinity {
    private static final List<EnumSet<ElementalBranch>> HUMAN_PAIRS = List.of(
            EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER),
            EnumSet.of(ElementalBranch.AIR, ElementalBranch.EARTH),
            EnumSet.of(ElementalBranch.AIR, ElementalBranch.FIRE),
            EnumSet.of(ElementalBranch.WATER, ElementalBranch.EARTH),
            EnumSet.of(ElementalBranch.WATER, ElementalBranch.FIRE),
            EnumSet.of(ElementalBranch.EARTH, ElementalBranch.FIRE)
    );

    private ElementalsRaceAffinity() {}

    public static MageRaceProfile resolve(String raceId) {
        String normalized = raceId == null ? "" : raceId.trim().toLowerCase();
        return switch (normalized) {
            case "tensura:elf" -> new MageRaceProfile(normalized, true, false, false, EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));
            case "tensura:human" -> new MageRaceProfile(normalized, true, true, false, EnumSet.noneOf(ElementalBranch.class));
            case "tensura:dwarf" -> new MageRaceProfile(normalized, true, false, false, EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH));
            case "tensura:beastfolk" -> new MageRaceProfile(normalized, true, false, true, EnumSet.of(ElementalBranch.WATER, ElementalBranch.AIR));
            default -> new MageRaceProfile(normalized, false, false, false, EnumSet.noneOf(ElementalBranch.class));
        };
    }

    public static EnumSet<ElementalBranch> starterBranches(MageRaceProfile profile, UUID playerId) {
        if (profile == null || !profile.supported()) {
            return EnumSet.noneOf(ElementalBranch.class);
        }
        if (!profile.human()) {
            return EnumSet.copyOf(profile.fixedStarters());
        }
        int index = Math.floorMod(playerId.hashCode(), HUMAN_PAIRS.size());
        return EnumSet.copyOf(HUMAN_PAIRS.get(index));
    }
}
```

```java
public static final DeferredHolder<AttachmentType<?>, AttachmentType<ElementalsMageData>> ELEMENTALS_MAGE =
        ATTACHMENTS.register("elementals_mage", () ->
                AttachmentType.builder(ElementalsMageData::new)
                        .serialize(ElementalsMageSerializer.INSTANCE)
                        .build());
```

```java
private static int[] ordinals(EnumSet<ElementalBranch> branches) {
    return branches.stream().mapToInt(Enum::ordinal).toArray();
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsRaceAffinityTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/elementals/ElementalBranch.java src/main/java/tong/statmod/integration/elementals/ElementState.java src/main/java/tong/statmod/integration/elementals/MageRaceProfile.java src/main/java/tong/statmod/integration/elementals/ElementalsMageData.java src/main/java/tong/statmod/integration/elementals/ElementalsRaceAffinity.java src/main/java/tong/statmod/storage/ModAttachments.java src/test/java/tong/statmod/integration/elementals/ElementalsRaceAffinityTest.java
git commit -m "Add Elementals mage state and race affinity rules"
```

### Task 2: Add Progression Thresholds and Exact Perk Bindings

**Files:**
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsPerkBindings.java`
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsMageRules.java`
- Test: `src/test/java/tong/statmod/integration/elementals/ElementalsPerkBindingsTest.java`
- Test: `src/test/java/tong/statmod/integration/elementals/ElementalsMageRulesTest.java`

- [ ] **Step 1: Write the failing rules and perk-binding tests**

```java
package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsPerkBindingsTest {
    @Test
    void baseMasteryUsesExistingElementMasteryPerks() {
        assertEquals(Perk.AIR_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.AIR));
        assertEquals(Perk.WATER_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.WATER));
        assertEquals(Perk.EARTH_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.EARTH));
        assertEquals(Perk.FIRE_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.FIRE));
    }

    @Test
    void broaderUnlocksUseConcreteExistingPerkIds() {
        assertEquals(Set.of(Perk.ERUDITION_CORE.id, Perk.FIRE_CORE.id),
                ElementalsPerkBindings.thirdUnlockPerks(ElementalBranch.FIRE));
        assertEquals(Set.of(Perk.ERUDITION_MASTERY.id, Perk.WATER_ACTIVE.id),
                ElementalsPerkBindings.fourthUnlockPerks(ElementalBranch.WATER));
    }

    @Test
    void rareRewardsUseFreeGrantedExistingTreePerksWithoutAutoGrantHooks() {
        assertEquals(Perk.AIR_TRANSCENDENCE, ElementalsPerkBindings.rareRewardPerk(ElementalBranch.LIGHTNING));
        assertEquals(Perk.WILL_TRANSCENDENCE, ElementalsPerkBindings.rareRewardPerk(ElementalBranch.BLOOD));
    }
}
```

```java
package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsMageRulesTest {
    private static IntUnaryOperator levels(int... values) {
        return index -> index >= 0 && index < values.length ? values[index] : 0;
    }

    @Test
    void mageAwakeningUsesTheApprovedThresholdBands() {
        MageRaceProfile elf = ElementalsRaceAffinity.resolve("tensura:elf");
        MageRaceProfile beastfolk = ElementalsRaceAffinity.resolve("tensura:beastfolk");
        assertTrue(ElementalsMageRules.canAwaken(elf, levels(0,0,0,0,0,0, 0,12,0,0,0,0,12,12,0,0,0,0,0,0,0,0,0)));
        assertFalse(ElementalsMageRules.canAwaken(beastfolk, levels(0,0,0,0,0,0, 0,12,0,0,0,0,12,12,0,0,0,0,0,0,0,0,0)));
    }

    @Test
    void startingElementBecomesMasteredOnlyWithStatsAndPerk() {
        IntUnaryOperator levels = levels(0,0,0,0,0,0, 0,10,0,0,18,0,0,14,0,6,0,0,0,0,0,0,0);
        assertEquals(ElementState.AWAKENED,
                ElementalsMageRules.stateForBaseBranch(ElementalBranch.FIRE, levels, Set.of()));
        assertEquals(ElementState.MASTERED,
                ElementalsMageRules.stateForBaseBranch(ElementalBranch.FIRE, levels, Set.of(Perk.FIRE_MASTERY.id)));
    }

    @Test
    void thirdAndFourthBaseUnlocksRespectHumanAndBeastfolkModifiers() {
        IntUnaryOperator humanLevels = levels(0,0,0,0,0,0, 0,18,18,22,22,0,0,14,0,18,0,0,0,0,0,0,0);
        MageRaceProfile human = ElementalsRaceAffinity.resolve("tensura:human");
        assertTrue(ElementalsMageRules.canUnlockThirdBase(human, ElementalBranch.EARTH, humanLevels, Set.of(Perk.ERUDITION_CORE.id, Perk.EARTH_CORE.id), 1));

        MageRaceProfile beastfolk = ElementalsRaceAffinity.resolve("tensura:beastfolk");
        assertFalse(ElementalsMageRules.canUnlockThirdBase(beastfolk, ElementalBranch.EARTH, humanLevels, Set.of(Perk.ERUDITION_CORE.id, Perk.EARTH_CORE.id), 1));
    }

    @Test
    void rareGrimoireThresholdsUseTheApprovedPrimaryStats() {
        IntUnaryOperator lightning = levels(0,0,0,0,0,0, 0,20,0,0,0,0,0,20,0,18,0,0,0,0,0,0,0);
        IntUnaryOperator blood = levels(0,0,0,0,0,0, 0,20,0,0,0,0,0,0,0,18,0,0,0,0,0,0,20);
        assertTrue(ElementalsMageRules.canUseRareGrimoire(ElementalBranch.LIGHTNING, lightning));
        assertTrue(ElementalsMageRules.canUseRareGrimoire(ElementalBranch.BLOOD, blood));
    }
}
```

- [ ] **Step 2: Run the targeted tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsPerkBindingsTest --tests tong.statmod.integration.elementals.ElementalsMageRulesTest`  
Expected: FAIL with missing `ElementalsPerkBindings` and `ElementalsMageRules`

- [ ] **Step 3: Write the exact binding map and pure progression rules**

```java
package tong.statmod.integration.elementals;

import tong.statmod.perks.Perk;

import java.util.Set;

public final class ElementalsPerkBindings {
    private ElementalsPerkBindings() {}

    public static Perk masteryPerk(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> Perk.AIR_MASTERY;
            case WATER -> Perk.WATER_MASTERY;
            case EARTH -> Perk.EARTH_MASTERY;
            case FIRE -> Perk.FIRE_MASTERY;
            default -> throw new IllegalArgumentException("No base mastery perk for " + branch);
        };
    }

    public static Set<Integer> thirdUnlockPerks(ElementalBranch branch) {
        return Set.of(Perk.ERUDITION_CORE.id, branchCorePerk(branch).id);
    }

    public static Set<Integer> fourthUnlockPerks(ElementalBranch branch) {
        return Set.of(Perk.ERUDITION_MASTERY.id, branchActivePerk(branch).id);
    }

    public static Perk rareRewardPerk(ElementalBranch branch) {
        return switch (branch) {
            case LIGHTNING -> Perk.AIR_TRANSCENDENCE;
            case BLOOD -> Perk.WILL_TRANSCENDENCE;
            default -> throw new IllegalArgumentException("No rare reward perk for " + branch);
        };
    }

    private static Perk branchCorePerk(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> Perk.AIR_CORE;
            case WATER -> Perk.WATER_CORE;
            case EARTH -> Perk.EARTH_CORE;
            case FIRE -> Perk.FIRE_CORE;
            default -> throw new IllegalArgumentException("No core perk for " + branch);
        };
    }

    private static Perk branchActivePerk(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> Perk.AIR_ACTIVE;
            case WATER -> Perk.WATER_ACTIVE;
            case EARTH -> Perk.EARTH_ACTIVE;
            case FIRE -> Perk.FIRE_ACTIVE;
            default -> throw new IllegalArgumentException("No active perk for " + branch);
        };
    }
}
```

```java
package tong.statmod.integration.elementals;

import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

import java.util.Set;
import java.util.function.IntUnaryOperator;

public final class ElementalsMageRules {
    private ElementalsMageRules() {}

    public static int magicalTotal(IntUnaryOperator levels) {
        return level(levels, StatType.ARCANE_POWER)
                + level(levels, StatType.WATER_AFFINITY)
                + level(levels, StatType.EARTH_AFFINITY)
                + level(levels, StatType.FIRE_AFFINITY)
                + level(levels, StatType.AIR_AFFINITY)
                + level(levels, StatType.CASTING_SPEED)
                + level(levels, StatType.MANA_POOL)
                + level(levels, StatType.ERUDITION)
                + level(levels, StatType.MAGIC_RESISTANCE)
                + level(levels, StatType.WILLPOWER);
    }

    public static boolean canAwaken(MageRaceProfile profile, IntUnaryOperator levels) {
        int minimum = profile != null && profile.beastfolk() ? 13 : 12;
        int totalRequirement = profile != null && profile.beastfolk() ? 40 : 36;
        int qualifiedStats = 0;
        for (StatType stat : new StatType[]{StatType.ARCANE_POWER, StatType.CASTING_SPEED, StatType.MANA_POOL, StatType.ERUDITION, StatType.MAGIC_RESISTANCE, StatType.WILLPOWER}) {
            if (level(levels, stat) >= minimum) qualifiedStats++;
        }
        return profile != null && profile.supported() && qualifiedStats >= 2 && magicalTotal(levels) >= totalRequirement;
    }

    public static ElementState stateForBaseBranch(ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks) {
        if (!branch.isBaseBranch()) return ElementState.LOCKED;
        int primary = level(levels, primaryStat(branch));
        int secondary = level(levels, secondaryCoreStat(branch));
        boolean mastered = primary >= 18
                && secondary >= 14
                && magicalTotal(levels) >= 48
                && unlockedPerks.contains(ElementalsPerkBindings.masteryPerk(branch).id);
        return mastered ? ElementState.MASTERED : ElementState.AWAKENED;
    }

    public static boolean canUnlockThirdBase(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks, int masteredBaseCount) {
        int totalRequirement = profile != null && profile.human() ? 56 : profile != null && profile.beastfolk() ? 66 : 60;
        return branch.isBaseBranch()
                && masteredBaseCount >= 1
                && level(levels, primaryStat(branch)) >= 22
                && level(levels, StatType.ERUDITION) >= 18
                && level(levels, StatType.ARCANE_POWER) >= 18
                && magicalTotal(levels) >= totalRequirement
                && unlockedPerks.containsAll(ElementalsPerkBindings.thirdUnlockPerks(branch));
    }

    public static boolean canUnlockFourthBase(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks, int masteredCount) {
        int totalRequirement = profile != null && profile.human() ? 72 : profile != null && profile.beastfolk() ? 82 : 76;
        return branch.isBaseBranch()
                && masteredCount >= 2
                && level(levels, primaryStat(branch)) >= 26
                && level(levels, StatType.ERUDITION) >= 22
                && level(levels, StatType.ARCANE_POWER) >= 22
                && magicalTotal(levels) >= totalRequirement
                && unlockedPerks.containsAll(ElementalsPerkBindings.fourthUnlockPerks(branch));
    }

    public static boolean canUseRareGrimoire(ElementalBranch branch, IntUnaryOperator levels) {
        return switch (branch) {
            case LIGHTNING -> level(levels, StatType.CASTING_SPEED) >= 20
                    && level(levels, StatType.ARCANE_POWER) >= 20
                    && magicalTotal(levels) >= 58;
            case BLOOD -> level(levels, StatType.WILLPOWER) >= 20
                    && level(levels, StatType.ARCANE_POWER) >= 20
                    && magicalTotal(levels) >= 58;
            default -> false;
        };
    }

    private static int level(IntUnaryOperator levels, StatType stat) {
        return levels.applyAsInt(stat.index);
    }

    private static StatType primaryStat(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> StatType.AIR_AFFINITY;
            case WATER -> StatType.WATER_AFFINITY;
            case EARTH -> StatType.EARTH_AFFINITY;
            case FIRE -> StatType.FIRE_AFFINITY;
            case LIGHTNING -> StatType.CASTING_SPEED;
            case BLOOD -> StatType.WILLPOWER;
        };
    }

    private static StatType secondaryCoreStat(ElementalBranch branch) {
        return switch (branch) {
            case AIR, FIRE -> StatType.CASTING_SPEED;
            case WATER, EARTH -> StatType.MANA_POOL;
            default -> StatType.ARCANE_POWER;
        };
    }
}
```

- [ ] **Step 4: Run the targeted tests to verify they pass**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsPerkBindingsTest --tests tong.statmod.integration.elementals.ElementalsMageRulesTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/elementals/ElementalsPerkBindings.java src/main/java/tong/statmod/integration/elementals/ElementalsMageRules.java src/test/java/tong/statmod/integration/elementals/ElementalsPerkBindingsTest.java src/test/java/tong/statmod/integration/elementals/ElementalsMageRulesTest.java
git commit -m "Add Elementals progression rules and perk bindings"
```

### Task 3: Bridge `STAT Mod` Mage State into Native `Elementals` Runtime

**Files:**
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsRuntimePort.java`
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsRuntimeBridge.java`
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsCompat.java`
- Modify: `src/main/java/tong/statmod/STATMod.java`
- Test: `src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java`

- [ ] **Step 1: Write the failing reconciliation test**

```java
package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsCompatTest {
    private static final class FakeRuntime implements ElementalsRuntimePort {
        private EnumSet<ElementalBranch> allowedBranches = EnumSet.noneOf(ElementalBranch.class);

        @Override
        public void setAllowedBranches(EnumSet<ElementalBranch> branches) {
            allowedBranches = EnumSet.copyOf(branches);
        }
    }

    @Test
    void awakeningElfAddsAirAndWaterAsAwakenedBranches() {
        FakeRuntime runtime = new FakeRuntime();
        ElementalsMageData data = new ElementalsMageData();
        MageRaceProfile profile = ElementalsRaceAffinity.resolve("tensura:elf");

        ElementalsCompat.reconcileMageState(
                profile,
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                levels -> switch (levels) {
                    case 7, 13, 14 -> 12;
                    default -> 0;
                },
                Set.of(),
                data,
                runtime
        );

        assertTrue(data.mageAwakened());
        assertEquals(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER), runtime.allowedBranches);
    }

    @Test
    void unsupportedRacesLoseManagedBranches() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.allowedBranches = EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER, ElementalBranch.BLOOD);
        ElementalsMageData data = new ElementalsMageData();
        data.setUnlockedBranches(EnumSet.copyOf(runtime.allowedBranches));

        ElementalsCompat.reconcileMageState(
                ElementalsRaceAffinity.resolve("tensura:slime"),
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                index -> 0,
                Set.of(),
                data,
                runtime
        );

        assertEquals(EnumSet.noneOf(ElementalBranch.class), runtime.allowedBranches);
    }
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsCompatTest`  
Expected: FAIL with missing `ElementalsCompat` and `ElementalsRuntimeBridge`

- [ ] **Step 3: Write the runtime adapter and server-side reconciliation loop**

```java
package tong.statmod.integration.elementals;

import java.util.EnumSet;

public interface ElementalsRuntimePort {
    void setAllowedBranches(EnumSet<ElementalBranch> branches);
}
```

```java
package tong.statmod.integration.elementals;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.elements.Element;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumSet;

public class ElementalsRuntimeBridge implements ElementalsRuntimePort {
    @Override
    public void setAllowedBranches(EnumSet<ElementalBranch> branches) {
        throw new UnsupportedOperationException("Use the server overload");
    }

    public EnumSet<ElementalBranch> branches(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        EnumSet<ElementalBranch> result = EnumSet.noneOf(ElementalBranch.class);
        for (Element element : new ArrayList<>(data.elements)) {
            result.add(ElementalBranch.valueOf(element.getName().toUpperCase()));
        }
        return result;
    }

    public void setAllowedBranches(ServerPlayer player, EnumSet<ElementalBranch> allowed) {
        Bender bender = Bender.getBender(player);
        for (ElementalBranch branch : ElementalBranch.values()) {
            Element element = Element.getElement(branch.elementalsName());
            if (element == null) continue;
            if (allowed.contains(branch) && !bender.hasElement(element)) {
                bender.addElement(element, false);
            }
            if (!allowed.contains(branch) && bender.hasElement(element)) {
                bender.removeElement(element, false);
            }
        }
        bender.bindDefaultAbilities();
        bender.syncElements();
    }

    public float chi(ServerPlayer player) {
        return PlayerData.get(player).chi;
    }

    public float xp(ServerPlayer player) {
        return PlayerData.get(player).xp;
    }

    public int level(ServerPlayer player) {
        return PlayerData.get(player).level;
    }

    public void setXp(ServerPlayer player, float value) {
        PlayerData.get(player).xp = Math.max(0.0f, value);
    }

    public void setChi(ServerPlayer player, float value) {
        PlayerData.get(player).chi = Math.max(0.0f, value);
        Bender.getBender(player).syncChi();
    }

    public ElementalBranch activeBranch(ServerPlayer player) {
        Element active = Bender.getBender(player).getElement();
        return active == null ? null : ElementalBranch.valueOf(active.getName().toUpperCase());
    }
}
```

```java
package tong.statmod.integration.elementals;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.network.SyncHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

public final class ElementalsCompat {
    private static final ElementalsRuntimeBridge RUNTIME = new ElementalsRuntimeBridge();

    private ElementalsCompat() {}

    public static void init() {
        if (!ModList.get().isLoaded("elementals")) {
            STATMod.LOGGER.info("Elementals not detected, skipping ElementalsCompat");
            return;
        }
        NeoForge.EVENT_BUS.register(ElementalsCompat.class);
        NeoForge.EVENT_BUS.register(ElementalsCombatScalingHandler.class);
        STATMod.LOGGER.info("Elementals integration loaded");
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        reconcilePlayer(player);
    }

    public static void reconcilePlayer(ServerPlayer player) {
        PlayerStatData statData = player.getData(ModAttachments.STATS);
        ElementalsMageData data = player.getData(ModAttachments.ELEMENTALS_MAGE);
        MageRaceProfile profile = ElementalsRaceAffinity.resolve(PlayerDataBridge.getRaceId(player));
        Set<Integer> unlockedPerks = new HashSet<>();
        for (int id : statData.getUnlockedPerks()) {
            unlockedPerks.add(id);
        }
        EnumSet<ElementalBranch> allowed = reconcileMageState(profile, player.getUUID(), statData::getLevel, unlockedPerks, data, null);
        RUNTIME.setAllowedBranches(player, allowed);
        SyncHelper.syncPerks(player);
        SyncHelper.syncStats(player);
    }

    static EnumSet<ElementalBranch> reconcileMageState(MageRaceProfile profile, UUID playerId, IntUnaryOperator levels, Set<Integer> unlockedPerks, ElementalsMageData data, ElementalsRuntimePort runtime) {
        if (profile == null || !profile.supported()) {
            data.setMageAwakened(false);
            data.setUnlockedBranches(EnumSet.noneOf(ElementalBranch.class));
            if (runtime != null) runtime.setAllowedBranches(EnumSet.noneOf(ElementalBranch.class));
            return EnumSet.noneOf(ElementalBranch.class);
        }

        if (!data.mageAwakened() && ElementalsMageRules.canAwaken(profile, levels)) {
            data.setMageAwakened(true);
            data.setStarterBranches(ElementalsRaceAffinity.starterBranches(profile, playerId));
        }

        EnumSet<ElementalBranch> allowed = data.mageAwakened()
                ? EnumSet.copyOf(data.starterBranches())
                : EnumSet.noneOf(ElementalBranch.class);

        int masteredBaseCount = 0;
        for (ElementalBranch branch : allowed) {
            if (branch.isBaseBranch() && ElementalsMageRules.stateForBaseBranch(branch, levels, unlockedPerks) == ElementState.MASTERED) {
                masteredBaseCount++;
            }
        }

        for (ElementalBranch branch : ElementalBranch.values()) {
            if (!branch.isBaseBranch() || allowed.contains(branch)) {
                continue;
            }
            if (ElementalsMageRules.canUnlockThirdBase(profile, branch, levels, unlockedPerks, masteredBaseCount)
                    || ElementalsMageRules.canUnlockFourthBase(profile, branch, levels, unlockedPerks, masteredBaseCount)) {
                allowed.add(branch);
            }
        }

        allowed.addAll(data.rewardedRareBranches());
        data.setUnlockedBranches(allowed);
        if (runtime != null) runtime.setAllowedBranches(allowed);
        return allowed;
    }

    static ElementalBranch activeBranch(Player player) {
        return player instanceof ServerPlayer serverPlayer ? RUNTIME.activeBranch(serverPlayer) : null;
    }

    static ElementState stateFor(Player player, ElementalBranch branch) {
        Set<Integer> unlockedPerks = new HashSet<>();
        for (int id : player.getData(ModAttachments.STATS).getUnlockedPerks()) {
            unlockedPerks.add(id);
        }
        if (player.getData(ModAttachments.ELEMENTALS_MAGE).unlockedBranches().contains(branch) && branch.isBaseBranch()) {
            return ElementalsMageRules.stateForBaseBranch(branch, player.getData(ModAttachments.STATS)::getLevel, unlockedPerks);
        }
        return player.getData(ModAttachments.ELEMENTALS_MAGE).unlockedBranches().contains(branch)
                ? ElementState.AWAKENED
                : ElementState.LOCKED;
    }
}
```

```java
if (ModList.get().isLoaded("elementals")) {
    ElementalsCompat.init();
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsCompatTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/elementals/ElementalsRuntimePort.java src/main/java/tong/statmod/integration/elementals/ElementalsRuntimeBridge.java src/main/java/tong/statmod/integration/elementals/ElementalsCompat.java src/main/java/tong/statmod/STATMod.java src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java
git commit -m "Bridge STAT Mod mage state into Elementals runtime"
```

### Task 4: Add Awakened Penalties, Beastfolk Progression Slowdown, and Rare-Branch Costs

**Files:**
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsPenaltyModel.java`
- Create: `src/main/java/tong/statmod/integration/elementals/ElementalsCombatScalingHandler.java`
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsCompat.java`
- Test: `src/test/java/tong/statmod/integration/elementals/ElementalsPenaltyModelTest.java`

- [ ] **Step 1: Write the failing penalty-model test**

```java
package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementalsPenaltyModelTest {
    @Test
    void awakenedBranchesUseTheApprovedBasePenalty() {
        assertEquals(0.80f, ElementalsPenaltyModel.damageMultiplier(ElementState.AWAKENED), 0.0001f);
        assertEquals(1.25f, ElementalsPenaltyModel.chiCostMultiplier(ElementState.AWAKENED, false, 0), 0.0001f);
    }

    @Test
    void rareBranchesIncreaseCostsAndSlowOtherBranchProgression() {
        assertEquals(1.50f, ElementalsPenaltyModel.chiCostMultiplier(ElementState.AWAKENED, true, 1), 0.0001f);
        assertEquals(0.68f, ElementalsPenaltyModel.progressionMultiplier(true, ElementalBranch.FIRE, EnumSet.of(ElementalBranch.LIGHTNING, ElementalBranch.BLOOD)), 0.0001f);
    }
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsPenaltyModelTest`  
Expected: FAIL with missing `ElementalsPenaltyModel`

- [ ] **Step 3: Write the penalty model and apply it through chi/xp reconciliation plus outgoing damage scaling**

```java
package tong.statmod.integration.elementals;

import java.util.EnumSet;

public final class ElementalsPenaltyModel {
    private ElementalsPenaltyModel() {}

    public static float damageMultiplier(ElementState state) {
        return state == ElementState.AWAKENED ? 0.80f : 1.0f;
    }

    public static float chiCostMultiplier(ElementState state, boolean rareBranch, int rareOwned) {
        float multiplier = state == ElementState.AWAKENED ? 1.25f : 1.0f;
        if (rareBranch) {
            multiplier += 0.25f;
        }
        if (rareOwned > 1) {
            multiplier += 0.10f;
        }
        return multiplier;
    }

    public static float progressionMultiplier(boolean beastfolk, ElementalBranch activeBranch, EnumSet<ElementalBranch> rareOwned) {
        float multiplier = beastfolk ? 0.85f : 1.0f;
        if (activeBranch != ElementalBranch.LIGHTNING && rareOwned.contains(ElementalBranch.LIGHTNING)) multiplier *= 0.80f;
        if (activeBranch != ElementalBranch.BLOOD && rareOwned.contains(ElementalBranch.BLOOD)) multiplier *= 0.80f;
        return multiplier;
    }
}
```

```java
// inside ElementalsCompat.onPlayerTick after reconcilePlayer(player)
ElementalsMageData mageData = player.getData(ModAttachments.ELEMENTALS_MAGE);
ElementalsRuntimeBridge runtime = RUNTIME;
ElementalBranch activeBranch = runtime.activeBranch(player);
float currentChi = runtime.chi(player);
float currentXp = runtime.xp(player);
int currentLevel = runtime.level(player);

if (mageData.lastSeenChi() > currentChi && activeBranch != null) {
    ElementState state = mageStateFor(player, activeBranch);
    boolean rare = activeBranch == ElementalBranch.LIGHTNING || activeBranch == ElementalBranch.BLOOD;
    float spent = mageData.lastSeenChi() - currentChi;
    float extra = spent * (ElementalsPenaltyModel.chiCostMultiplier(state, rare, mageData.rewardedRareBranches().size()) - 1.0f);
    runtime.setChi(player, Math.max(0.0f, currentChi - extra));
}

if (mageData.lastSeenLevel() == currentLevel && currentXp > mageData.lastSeenXp() && activeBranch != null) {
    float gained = currentXp - mageData.lastSeenXp();
    float kept = gained * ElementalsPenaltyModel.progressionMultiplier(
            ElementalsRaceAffinity.resolve(PlayerDataBridge.getRaceId(player)).beastfolk(),
            activeBranch,
            mageData.rewardedRareBranches());
    runtime.setXp(player, mageData.lastSeenXp() + kept);
}

mageData.setLastSeenChi(runtime.chi(player));
mageData.setLastSeenXp(runtime.xp(player));
mageData.setLastSeenLevel(runtime.level(player));
```

```java
package tong.statmod.integration.elementals;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import tong.statmod.storage.ModAttachments;

public final class ElementalsCombatScalingHandler {
    private ElementalsCombatScalingHandler() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        Entity owner = source.getEntity();
        if (!(owner instanceof Player player)) return;
        if (direct == null || !direct.getClass().getName().startsWith("dev.saperate.elementals.")) return;

        ElementalBranch active = ElementalsCompat.activeBranch(player);
        if (active == null) return;
        ElementState state = ElementalsCompat.stateFor(player, active);
        event.setAmount(event.getAmount() * ElementalsPenaltyModel.damageMultiplier(state));
    }
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsPenaltyModelTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/elementals/ElementalsPenaltyModel.java src/main/java/tong/statmod/integration/elementals/ElementalsCombatScalingHandler.java src/main/java/tong/statmod/integration/elementals/ElementalsCompat.java src/test/java/tong/statmod/integration/elementals/ElementalsPenaltyModelTest.java
git commit -m "Add Elementals awakened and rare-branch penalty model"
```

### Task 5: Add Rare Grimoires and Finalize Unlock Flow

**Files:**
- Create: `src/main/java/tong/statmod/item/ElementalGrimoireItem.java`
- Modify: `src/main/java/tong/statmod/item/ModItems.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Create: `src/main/resources/assets/statmod/models/item/blood_grimoire.json`
- Create: `src/main/resources/assets/statmod/models/item/lightning_grimoire.json`
- Test: `src/test/java/tong/statmod/item/ElementalGrimoireItemTest.java`

- [ ] **Step 1: Write the failing grimoire test**

```java
package tong.statmod.item;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.elementals.ElementalBranch;
import tong.statmod.integration.elementals.ElementalsMageData;
import tong.statmod.integration.elementals.ElementalsPerkBindings;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalGrimoireItemTest {
    @Test
    void successfulRareGrimoireMarksRareBranchAndFreeGrantedPerk() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 20);
        statData.setLevel(13, 20);
        statData.setLevel(15, 18);
        ElementalsMageData mageData = new ElementalsMageData();

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(ElementalBranch.LIGHTNING, statData, mageData);

        assertTrue(consumed);
        assertTrue(mageData.rewardedRareBranches().contains(ElementalBranch.LIGHTNING));
        assertTrue(statData.isPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(ElementalBranch.LIGHTNING).id));
    }

    @Test
    void failingRareGrimoireDoesNotMutateState() {
        PlayerStatData statData = new PlayerStatData();
        ElementalsMageData mageData = new ElementalsMageData();

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(ElementalBranch.BLOOD, statData, mageData);

        assertFalse(consumed);
        assertFalse(mageData.rewardedRareBranches().contains(ElementalBranch.BLOOD));
    }
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.item.ElementalGrimoireItemTest`  
Expected: FAIL with missing `ElementalGrimoireItem`

- [ ] **Step 3: Write the custom grimoire item, register it, and add basic resources**

```java
package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tong.statmod.integration.elementals.ElementalBranch;
import tong.statmod.integration.elementals.ElementalsCompat;
import tong.statmod.integration.elementals.ElementalsMageData;
import tong.statmod.integration.elementals.ElementalsMageRules;
import tong.statmod.integration.elementals.ElementalsPerkBindings;
import tong.statmod.network.SyncHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.EnumSet;

public class ElementalGrimoireItem extends Item {
    private final ElementalBranch branch;

    public ElementalGrimoireItem(ElementalBranch branch, Properties properties) {
        super(properties);
        this.branch = branch;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        PlayerStatData statData = serverPlayer.getData(ModAttachments.STATS);
        ElementalsMageData mageData = serverPlayer.getData(ModAttachments.ELEMENTALS_MAGE);
        if (!tryUnlock(branch, statData, mageData)) {
            serverPlayer.sendSystemMessage(Component.translatable("item.statmod." + branch.name().toLowerCase() + "_grimoire.denied"));
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        serverPlayer.getItemInHand(hand).shrink(1);
        ElementalsCompat.reconcilePlayer(serverPlayer);
        SyncHelper.syncPerks(serverPlayer);
        serverPlayer.sendSystemMessage(Component.translatable("item.statmod." + branch.name().toLowerCase() + "_grimoire.unlocked"));
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    static boolean tryUnlockForTests(ElementalBranch branch, PlayerStatData statData, ElementalsMageData mageData) {
        return tryUnlock(branch, statData, mageData);
    }

    private static boolean tryUnlock(ElementalBranch branch, PlayerStatData statData, ElementalsMageData mageData) {
        if (!ElementalsMageRules.canUseRareGrimoire(branch, statData::getLevel)) {
            return false;
        }
        if (mageData.rewardedRareBranches().contains(branch)) {
            return false;
        }

        EnumSet<ElementalBranch> rewards = mageData.rewardedRareBranches();
        rewards.add(branch);
        mageData.setRewardedRareBranches(rewards);

        EnumSet<ElementalBranch> unlocked = mageData.unlockedBranches();
        unlocked.add(branch);
        mageData.setUnlockedBranches(unlocked);

        statData.markPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(branch).id);
        return true;
    }
}
```

```java
public static final Supplier<Item> BLOOD_GRIMOIRE = ITEMS.register("blood_grimoire",
        () -> new ElementalGrimoireItem(ElementalBranch.BLOOD, new Item.Properties().stacksTo(1)));
public static final Supplier<Item> LIGHTNING_GRIMOIRE = ITEMS.register("lightning_grimoire",
        () -> new ElementalGrimoireItem(ElementalBranch.LIGHTNING, new Item.Properties().stacksTo(1)));
```

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/enchanted_book"
  }
}
```

```json
{
  "item.statmod.blood_grimoire": "Blood Grimoire",
  "item.statmod.lightning_grimoire": "Lightning Grimoire",
  "item.statmod.blood_grimoire.denied": "You are not ready to bind blood.",
  "item.statmod.blood_grimoire.unlocked": "Blood bending has awakened.",
  "item.statmod.lightning_grimoire.denied": "You are not ready to bind lightning.",
  "item.statmod.lightning_grimoire.unlocked": "Lightning bending has awakened."
}
```

- [ ] **Step 4: Run the targeted test, then run the full build**

Run: `.\gradlew.bat test --tests tong.statmod.item.ElementalGrimoireItemTest`  
Expected: PASS

Run: `.\gradlew.bat build`  
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/item/ElementalGrimoireItem.java src/main/java/tong/statmod/item/ModItems.java src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json src/main/resources/assets/statmod/models/item/blood_grimoire.json src/main/resources/assets/statmod/models/item/lightning_grimoire.json src/test/java/tong/statmod/item/ElementalGrimoireItemTest.java
git commit -m "Add Elementals rare grimoire unlock flow"
```

---

## Self-Review

### Spec coverage

- source of truth belongs to `STAT Mod`: Task 1, Task 2, Task 3
- supported race set reduced to `human`, `elf`, `dwarf`, `beastfolk`: Task 1
- deterministic human 2-element start and fixed race starters: Task 1
- auto mage awakening: Task 2, Task 3
- `locked` / `awakened` / `mastered` branch states: Task 2, Task 3
- starter mastery thresholds: Task 2
- third/fourth base element thresholds with human/beastfolk modifiers: Task 2, Task 3
- native `Elementals` UI/keybinds preserved: Task 3
- awakened penalties `-20%` power and `+25%` resource cost: Task 4
- beastfolk slower progression: Task 4
- rare `blood` and `lightning` grimoires with permanent unlocks: Task 5
- rare-branch higher cost and slower other-element progression: Task 4, Task 5
- no `metal` implementation: preserved by file scope and branch enum usage
- no Mahou/Tensura replacement: preserved by touching only `integration/elementals` plus shared sync/items

### Placeholder scan

- no `TODO`
- no `TBD`
- no “similar to Task N”
- concrete perk ids are mapped in `ElementalsPerkBindings`
- concrete commands are provided for every test/build step

### Type consistency

- canonical branch type is always `ElementalBranch`
- canonical state type is always `ElementState`
- race resolution always flows through `ElementalsRaceAffinity.resolve(...)`
- perk gating always flows through `ElementalsPerkBindings`
- runtime `Elementals` calls are isolated in `ElementalsRuntimeBridge`

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-19-elementals-mage-class-implementation.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
