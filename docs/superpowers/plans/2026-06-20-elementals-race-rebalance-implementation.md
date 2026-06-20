# Elementals Race Rebalance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebalance `Elementals` mage progression around the four supported races, add `metal` as a third rare branch, and keep all unlock logic stat-driven through `STAT Mod`.

**Architecture:** Keep the current architecture centered on `ElementalsRaceAffinity`, `MageRaceProfile`, `ElementalsMageRules`, `ElementalsCompat`, and the rare-grimoire item flow. Add race-aware threshold helpers instead of scattering race checks through runtime sync logic, and extend the rare-branch item/perk path to include `metal`.

**Tech Stack:** Java 21, NeoForge 1.21.1, JUnit 5, existing `STAT Mod` attachments/perk system, `Elementals` runtime bridge.

---

## File Map

- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalBranch.java`
  - add `METAL`
- Modify: `src/main/java/tong/statmod/integration/elementals/MageRaceProfile.java`
  - expose favored base branches and rare-branch affinity helpers
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsRaceAffinity.java`
  - keep four supported races and wire richer race profiles
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsMageRules.java`
  - implement race-aware awaken, mastery, third/fourth base, and rare branch thresholds
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsPerkBindings.java`
  - add `METAL` rare reward binding
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsRuntimeBridge.java`
  - ensure `METAL` can flow through runtime branch sync
- Modify: `src/main/java/tong/statmod/item/ElementalGrimoireItem.java`
  - support `METAL` unlock rules
- Modify: `src/main/java/tong/statmod/item/ModItems.java`
  - register `metal_grimoire`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
  - add `metal_grimoire` strings
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
  - add `metal_grimoire` strings
- Create: `src/main/resources/assets/statmod/models/item/metal_grimoire.json`
  - item model
- Modify: `src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java`
  - race starter / branch unlock regression coverage
- Create: `src/test/java/tong/statmod/integration/elementals/ElementalsMageRulesTest.java`
  - isolated threshold tests
- Modify: `src/test/java/tong/statmod/item/ElementalGrimoireItemTest.java`
  - `METAL` unlock tests

### Task 1: Lock the Rebalance Rules in Tests

**Files:**
- Modify: `src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java`
- Create: `src/test/java/tong/statmod/integration/elementals/ElementalsMageRulesTest.java`

- [ ] **Step 1: Write the failing race-aware threshold tests**

```java
package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsMageRulesTest {
    @Test
    void elfAwakensEarlierThanHumanAtSameStatSpread() {
        IntUnaryOperator levels = index -> switch (index) {
            case 7, 12 -> 12;
            case 13 -> 10;
            case 14 -> 12;
            default -> 0;
        };

        assertTrue(ElementalsMageRules.canAwaken(ElementalsRaceAffinity.resolve("tensura:elf"), levels));
        assertFalse(ElementalsMageRules.canAwaken(ElementalsRaceAffinity.resolve("tensura:human"), levels));
    }

    @Test
    void humanUnlocksThirdBaseEarlierThanDwarf() {
        IntUnaryOperator levels = index -> switch (index) {
            case 7 -> 16;
            case 10 -> 20;
            case 15 -> 16;
            default -> 0;
        };
        Set<Integer> perks = Set.of(Perk.ERUDITION_CORE.id, Perk.FIRE_CORE.id);

        assertTrue(ElementalsMageRules.canUnlockThirdBase(
                ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.FIRE,
                levels,
                perks,
                1));
        assertFalse(ElementalsMageRules.canUnlockThirdBase(
                ElementalsRaceAffinity.resolve("tensura:dwarf"),
                ElementalBranch.FIRE,
                levels,
                perks,
                1));
    }

    @Test
    void dwarfHasBestMetalGrimoireThreshold() {
        IntUnaryOperator levels = index -> switch (index) {
            case 7 -> 18;
            case 9 -> 20;
            case 10 -> 18;
            case 15 -> 4;
            default -> 0;
        };

        assertTrue(ElementalsMageRules.canUseRareGrimoire(
                ElementalsRaceAffinity.resolve("tensura:dwarf"),
                ElementalBranch.METAL,
                levels));
        assertFalse(ElementalsMageRules.canUseRareGrimoire(
                ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.METAL,
                levels));
    }
}
```

```java
@Test
void beastfolkNeedsHigherMagicalTotalForAwakening() {
    FakeRuntime runtime = new FakeRuntime();
    ElementalsMageData data = new ElementalsMageData();

    ElementalsCompat.reconcileMageState(
            ElementalsRaceAffinity.resolve("tensura:beastfolk"),
            UUID.fromString("00000000-0000-0000-0000-000000000020"),
            index -> switch (index) {
                case 7, 12 -> 13;
                case 13 -> 10;
                case 14 -> 6;
                default -> 0;
            },
            Set.of(),
            data,
            runtime
    );

    assertFalse(data.mageAwakened());
}
```

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsMageRulesTest --tests tong.statmod.integration.elementals.ElementalsCompatTest`

Expected: FAIL with missing `ElementalBranch.METAL`, wrong `canUseRareGrimoire(...)` signature, and threshold assertions failing under current generic rules.

- [ ] **Step 3: Implement the minimal race model and threshold changes**

```java
public record MageRaceProfile(
        String raceId,
        boolean supported,
        boolean human,
        boolean beastfolk,
        EnumSet<ElementalBranch> fixedStarters,
        EnumSet<ElementalBranch> favoredBaseBranches
) {
    public boolean favors(ElementalBranch branch) {
        return favoredBaseBranches.contains(branch);
    }
}
```

```java
public enum ElementalBranch {
    AIR("air", true),
    WATER("water", true),
    EARTH("earth", true),
    FIRE("fire", true),
    LIGHTNING("lightning", false),
    BLOOD("blood", false),
    METAL("metal", false);
}
```

```java
public static boolean canUseRareGrimoire(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels) {
    if (profile == null || branch == null || !profile.supported()) {
        return false;
    }
    return switch (branch) {
        case LIGHTNING -> level(levels, StatType.CASTING_SPEED) >= (profile.raceId().equals("tensura:elf") ? 18 : 20 + (profile.human() ? 0 : 2))
                && level(levels, StatType.ARCANE_POWER) >= 20
                && magicalTotal(levels) >= (profile.raceId().equals("tensura:elf") ? 58 : profile.human() ? 60 : 64);
        case BLOOD -> level(levels, StatType.WILLPOWER) >= (profile.human() || profile.raceId().equals("tensura:dwarf") ? 20 : 22)
                && level(levels, StatType.ARCANE_POWER) >= 20
                && magicalTotal(levels) >= (profile.human() || profile.raceId().equals("tensura:dwarf") ? 60 : profile.beastfolk() ? 64 : 62);
        case METAL -> level(levels, StatType.EARTH_AFFINITY) >= (profile.raceId().equals("tensura:dwarf") ? 20 : profile.human() ? 22 : 24)
                && level(levels, StatType.FIRE_AFFINITY) >= (profile.raceId().equals("tensura:dwarf") ? 18 : profile.human() ? 20 : 22)
                && level(levels, StatType.ARCANE_POWER) >= (profile.raceId().equals("tensura:dwarf") ? 18 : 20)
                && magicalTotal(levels) >= (profile.raceId().equals("tensura:dwarf") ? 60 : profile.human() ? 66 : 72);
        default -> false;
    };
}
```

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsMageRulesTest --tests tong.statmod.integration.elementals.ElementalsCompatTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/elementals/ElementalBranch.java src/main/java/tong/statmod/integration/elementals/MageRaceProfile.java src/main/java/tong/statmod/integration/elementals/ElementalsRaceAffinity.java src/main/java/tong/statmod/integration/elementals/ElementalsMageRules.java src/test/java/tong/statmod/integration/elementals/ElementalsMageRulesTest.java src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java
git commit -m "Rebalance Elementals race thresholds and branch model"
```

### Task 2: Apply the Rebalance to Runtime Unlock Flow

**Files:**
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsCompat.java`
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsRuntimeBridge.java`
- Modify: `src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java`

- [ ] **Step 1: Write the failing unlock-flow regression tests**

```java
@Test
void humanUnlocksThirdBaseAtNewLowerThreshold() {
    FakeRuntime runtime = new FakeRuntime();
    ElementalsMageData data = new ElementalsMageData();
    data.setMageAwakened(true);
    data.setStarterBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));
    data.setUnlockedBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));

    ElementalsCompat.reconcileMageState(
            ElementalsRaceAffinity.resolve("tensura:human"),
            UUID.fromString("00000000-0000-0000-0000-000000000021"),
            index -> switch (index) {
                case 7 -> 16;
                case 10 -> 20;
                case 15 -> 16;
                default -> 0;
            },
            Set.of(Perk.ERUDITION_CORE.id, Perk.FIRE_CORE.id, Perk.AIR_MASTERY.id),
            data,
            runtime
    );

    assertTrue(runtime.allowedBranches.contains(ElementalBranch.FIRE));
}

@Test
void runtimeKeepsMetalWhenStatModAllowsIt() {
    FakeRuntime runtime = new FakeRuntime();
    ElementalsMageData data = new ElementalsMageData();
    data.setMageAwakened(true);
    data.setUnlockedBranches(EnumSet.of(ElementalBranch.EARTH, ElementalBranch.FIRE, ElementalBranch.METAL));

    ElementalsCompat.reconcileMageState(
            ElementalsRaceAffinity.resolve("tensura:dwarf"),
            UUID.fromString("00000000-0000-0000-0000-000000000022"),
            index -> 30,
            Set.of(),
            data,
            runtime
    );

    assertTrue(runtime.allowedBranches.contains(ElementalBranch.METAL));
}
```

- [ ] **Step 2: Run the compat tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsCompatTest`

Expected: FAIL with missing `METAL` reconciliation and old unlock behavior.

- [ ] **Step 3: Implement the minimal compat/runtime changes**

```java
if (data.mageAwakened()) {
    allowed.addAll(data.starterBranches());
    for (ElementalBranch branch : data.unlockedBranches()) {
        if (branch.isBaseBranch()) {
            allowed.add(branch);
        }
    }
}

allowed.addAll(data.rewardedRareBranches());
```

```java
public static ElementState stateForBaseBranch(ElementalBranch branch, MageRaceProfile profile, IntUnaryOperator levels, Set<Integer> unlockedPerks) {
    int supportRequirement = profile.favors(branch) ? 12 : 14;
    int magicalRequirement = profile.human() ? 46 : profile.beastfolk() ? 52 : 48;
    boolean mastered = level(levels, primaryStat(branch)) >= 18
            && level(levels, secondaryCoreStat(branch)) >= supportRequirement
            && magicalTotal(levels) >= magicalRequirement
            && unlockedPerks.contains(ElementalsPerkBindings.masteryPerk(branch).id);
    return mastered ? ElementState.MASTERED : ElementState.AWAKENED;
}
```

```java
Element element = Element.getElement(branch.elementalsName());
if (element == null) {
    continue;
}
```

The runtime bridge change here is intentionally minimal: once `ElementalBranch.METAL` exists, the existing iteration over all branches should start syncing it as long as `Elementals` exposes the element by name.

- [ ] **Step 4: Run the compat tests to verify they pass**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsCompatTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/elementals/ElementalsCompat.java src/main/java/tong/statmod/integration/elementals/ElementalsMageRules.java src/main/java/tong/statmod/integration/elementals/ElementalsRuntimeBridge.java src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java
git commit -m "Apply Elementals race rebalance to runtime unlock flow"
```

### Task 3: Add Metal Rare-Branch Unlocks

**Files:**
- Modify: `src/main/java/tong/statmod/integration/elementals/ElementalsPerkBindings.java`
- Modify: `src/main/java/tong/statmod/item/ElementalGrimoireItem.java`
- Modify: `src/main/java/tong/statmod/item/ModItems.java`
- Modify: `src/test/java/tong/statmod/item/ElementalGrimoireItemTest.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Create: `src/main/resources/assets/statmod/models/item/metal_grimoire.json`

- [ ] **Step 1: Write the failing metal grimoire tests**

```java
@Test
void dwarfCanUnlockMetalWithLowerThreshold() {
    PlayerStatData statData = new PlayerStatData();
    statData.setLevel(7, 18);
    statData.setLevel(9, 20);
    statData.setLevel(10, 18);
    statData.setLevel(15, 4);
    ElementalsMageData mageData = new ElementalsMageData();
    mageData.setMageAwakened(true);

    boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
            ElementalsRaceAffinity.resolve("tensura:dwarf"),
            ElementalBranch.METAL,
            statData,
            mageData
    );

    assertTrue(consumed);
    assertTrue(mageData.rewardedRareBranches().contains(ElementalBranch.METAL));
    assertTrue(statData.isPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(ElementalBranch.METAL).id));
}

@Test
void humanFailsMetalAtDwarfOnlyThreshold() {
    PlayerStatData statData = new PlayerStatData();
    statData.setLevel(7, 18);
    statData.setLevel(9, 20);
    statData.setLevel(10, 18);
    statData.setLevel(15, 4);
    ElementalsMageData mageData = new ElementalsMageData();
    mageData.setMageAwakened(true);

    boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
            ElementalsRaceAffinity.resolve("tensura:human"),
            ElementalBranch.METAL,
            statData,
            mageData
    );

    assertFalse(consumed);
}
```

- [ ] **Step 2: Run the grimoire tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.item.ElementalGrimoireItemTest`

Expected: FAIL with missing overloads for race-aware `tryUnlockForTests(...)`, missing `METAL`, and missing perk binding.

- [ ] **Step 3: Implement the metal rare-branch path**

```java
public static Perk rareRewardPerk(ElementalBranch branch) {
    return switch (branch) {
        case LIGHTNING -> Perk.AIR_TRANSCENDENCE;
        case BLOOD -> Perk.WILL_TRANSCENDENCE;
        case METAL -> Perk.EARTH_TRANSCENDENCE;
        default -> throw new IllegalArgumentException("No rare reward perk for " + branch);
    };
}
```

```java
static boolean tryUnlockForTests(MageRaceProfile profile, ElementalBranch branch, PlayerStatData statData, ElementalsMageData mageData) {
    return tryUnlock(profile, branch, statData, mageData);
}

private static boolean tryUnlock(MageRaceProfile profile, ElementalBranch branch, PlayerStatData statData, ElementalsMageData mageData) {
    if (!mageData.mageAwakened()) {
        return false;
    }
    if (!ElementalsMageRules.canUseRareGrimoire(profile, branch, statData::getLevel)) {
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
```

```java
public static final Supplier<Item> METAL_GRIMOIRE = ITEMS.register("metal_grimoire",
        () -> new ElementalGrimoireItem(ElementalBranch.METAL, new Item.Properties().stacksTo(1)));
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
  "item.statmod.metal_grimoire": "Metal Grimoire",
  "item.statmod.metal_grimoire.denied": "You are not ready to bind metal.",
  "item.statmod.metal_grimoire.unlocked": "Metal bending has awakened."
}
```

- [ ] **Step 4: Run the grimoire tests to verify they pass**

Run: `.\gradlew.bat test --tests tong.statmod.item.ElementalGrimoireItemTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/elementals/ElementalsPerkBindings.java src/main/java/tong/statmod/item/ElementalGrimoireItem.java src/main/java/tong/statmod/item/ModItems.java src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json src/main/resources/assets/statmod/models/item/metal_grimoire.json src/test/java/tong/statmod/item/ElementalGrimoireItemTest.java
git commit -m "Add Elementals metal grimoire progression path"
```

### Task 4: Verify the Whole Rebalance Slice

**Files:**
- No new production files
- Reuse: `src/test/java/tong/statmod/integration/elementals/ElementalsMageRulesTest.java`
- Reuse: `src/test/java/tong/statmod/integration/elementals/ElementalsCompatTest.java`
- Reuse: `src/test/java/tong/statmod/item/ElementalGrimoireItemTest.java`

- [ ] **Step 1: Run the full targeted Elementals suite**

Run: `.\gradlew.bat test --tests tong.statmod.integration.elementals.ElementalsMageRulesTest --tests tong.statmod.integration.elementals.ElementalsCompatTest --tests tong.statmod.item.ElementalGrimoireItemTest`

Expected: PASS

- [ ] **Step 2: Run the project build**

Run: `.\gradlew.bat build`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Review git diff before closing**

Run: `git diff --stat HEAD~3..HEAD`

Expected: only `integration/elementals`, item resources, tests, and the new plan/spec commits in scope.

- [ ] **Step 4: Commit any final cleanup only if required**

```bash
git status --short
```

Expected: no unstaged files from this feature. If cleanup changes were needed, commit them with:

```bash
git add <exact paths>
git commit -m "Polish Elementals race rebalance verification"
```

---

## Self-Review

### Spec coverage

- four supported races preserved: Task 1
- semi-rigid early/mid/late progression: Task 1 and Task 2
- human as best universal mage path: Task 1 and Task 2
- elf/dwarf specialization strength: Task 1
- beastfolk harder progression: Task 1
- race-aware awaken/mastered/third/fourth thresholds: Task 1 and Task 2
- `metal` added as rare branch: Task 1 and Task 3
- `metal` grimoire-first with dwarf-favored access: Task 3
- permanent rare unlock binding: Task 3
- native `Elementals` UX preserved: Task 2 and Task 3 by limiting scope to rules/runtime/items only

### Placeholder scan

- no `TODO`
- no `TBD`
- every test step includes concrete test code
- every verification step includes exact commands and expected outcomes
- no “similar to Task N” references for implementation details

### Type consistency

- canonical branch type remains `ElementalBranch`
- race context flows through `MageRaceProfile`
- threshold evaluation flows through `ElementalsMageRules`
- rare branch acquisition still flows through `ElementalGrimoireItem`
- runtime sync remains centered on `ElementalsCompat` and `ElementalsRuntimeBridge`

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-20-elementals-race-rebalance-implementation.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
