# Stat Family Perk Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first implementation slice for the stat-identity spec by making magical stats perk-bearing, introducing family metadata, creating canonical spell taxonomies for Tensura and Mahou, and migrating Puffish perk trees from stat tabs to family tabs.

**Architecture:** Extend the existing enum-backed stat and perk model instead of refactoring progression into a new registry. Keep `STAT Mod` as the only canonical owner of points, unlock validation, and spell-taxonomy decisions, while `Puffish Skills` remains a mirrored UI. Add explicit taxonomy classes for `Tensura` and `Mahou Tsukai` so later spell gates and perk rewards consume typed classifications instead of heuristics and ad hoc maps.

**Tech Stack:** Java 21, NeoForge 1.21.1, JUnit 5, static JSON data resources for Puffish Skills

---

## File Structure

### New units

- `src/main/java/tong/statmod/stats/StatFamily.java`
  - family enum for all 23 stats
- `src/main/java/tong/statmod/integration/tensura/TensuraSpellProfile.java`
  - typed description of a Tensura spell classification
- `src/main/java/tong/statmod/integration/tensura/TensuraSpellTaxonomy.java`
  - canonical Tensura spell classification table
- `src/main/java/tong/statmod/integration/mahou/MahouSpellProfile.java`
  - typed description of a Mahou spell classification
- `src/main/java/tong/statmod/integration/mahou/MahouSpellTaxonomy.java`
  - canonical Mahou spell classification table
- `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilder.java`
  - pure builder that derives family-tab Puffish JSON payloads from `Perk` and `StatType`
- `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeExporter.java`
  - writes checked-in Puffish family resources from the builder
- `src/test/java/tong/statmod/perks/MagicPerkCatalogTest.java`
  - validates magical perk coverage exists for all magical stats
- `src/test/java/tong/statmod/integration/tensura/TensuraSpellTaxonomyTest.java`
  - validates curated Tensura spell classifications
- `src/test/java/tong/statmod/integration/mahou/MahouSpellTaxonomyTest.java`
  - validates curated Mahou spell classifications
- `src/test/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilderTest.java`
  - validates family category layout and generated category ids

### Existing files to modify

- `src/main/java/tong/statmod/stats/StatType.java`
  - add `StatFamily` metadata and enable magical stats for perks
- `src/test/java/tong/statmod/StatTypeTest.java`
  - update `hasPerks()` and family expectations
- `src/main/java/tong/statmod/perks/Perk.java`
  - append magical perk definitions
- `src/test/java/tong/statmod/PerkManagerTest.java`
  - validate magical perks unlock through canonical `PerkManager`
- `src/main/java/tong/statmod/integration/tensura/TensuraSpellGate.java`
  - resolve spell rewards through taxonomy-driven mappings
- `src/main/java/tong/statmod/integration/mahou/MahouPerkMap.java`
  - replace loose element routing with taxonomy-driven stat routing
- `src/main/java/tong/statmod/integration/mahou/MahouElementMapper.java`
  - consult canonical Mahou taxonomy first, then fallback heuristic
- `src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java`
  - map perks into family categories instead of stat categories
- `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java`
  - mirror family totals and family category unlock states
- `src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java`
  - update expectations to family categories
- `src/test/java/tong/statmod/integration/puffish/PuffishUnlockServiceTest.java`
  - verify family totals do not bypass stat-specific canonical validation
- `src/main/resources/data/statmod/puffish_skills/config.json`
  - family category order
- `src/main/resources/data/statmod/puffish_skills/categories/**`
  - replace per-stat categories with family categories

---

### Task 1: Add Family Metadata and Make All 23 Stats Perk-Bearing

**Files:**
- Create: `src/main/java/tong/statmod/stats/StatFamily.java`
- Modify: `src/main/java/tong/statmod/stats/StatType.java`
- Modify: `src/test/java/tong/statmod/StatTypeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class StatTypeTest {
    @Test
    void magicalStatsNowParticipateInPerkProgression() {
        assertTrue(StatType.ARCANE_POWER.hasPerks());
        assertTrue(StatType.ERUDITION.hasPerks());
        assertTrue(StatType.MAGIC_RESISTANCE.hasPerks());
    }

    @Test
    void assignsEachStatToItsFamily() {
        assertEquals(StatFamily.FRONTLINE_PHYSICAL_COMBAT, StatType.BRUTE_FORCE.family());
        assertEquals(StatFamily.MAGICAL_CORE, StatType.ARCANE_POWER.family());
        assertEquals(StatFamily.ELEMENTAL_SPECIALIZATION, StatType.FIRE_AFFINITY.family());
        assertEquals(StatFamily.CRAFTING_SUPPORT, StatType.ALCHEMY.family());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.StatTypeTest`  
Expected: FAIL because `StatFamily` and `family()` do not exist, and magical `hasPerks()` still returns `false`

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.stats;

public enum StatFamily {
    FRONTLINE_PHYSICAL_COMBAT("frontline_physical_combat", "Front-Line Physical Combat"),
    RANGED_HUNT_CONTROL("ranged_hunt_control", "Ranged and Hunt Control"),
    MAGICAL_CORE("magical_core", "Magical Core"),
    ELEMENTAL_SPECIALIZATION("elemental_specialization", "Elemental Specialization"),
    MENTAL_PRESSURE_RESILIENCE("mental_pressure_resilience", "Mental Pressure and Resilience"),
    CRAFTING_SUPPORT("crafting_support", "Crafting, Provisioning, and Technical Support");

    public final String slug;
    public final String displayName;

    StatFamily(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }
}
```

```java
package tong.statmod.stats;

public enum StatType {
    BRUTE_FORCE(0, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Brute Force", "Damage bonus for heavy weapons"),
    BLADE_TECHNIQUE(1, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Blade Technique", "Precision and finesse with blades"),
    RAPIDITE(2, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Rapidité", "Attack speed and fluidity"),
    AGILITY(3, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Agility", "Movement speed and evasion"),
    PHYSICAL_RESISTANCE(4, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Physical Resistance", "Incoming damage reduction"),
    PHYSICAL_ENDURANCE(5, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Physical Endurance", "Stamina and absorption"),
    PRECISION(6, StatFamily.RANGED_HUNT_CONTROL, "Precision", "Ranged accuracy and critical hits"),
    ARCANE_POWER(7, StatFamily.MAGICAL_CORE, "Arcane Power", "Raw magical damage"),
    WATER_AFFINITY(8, StatFamily.ELEMENTAL_SPECIALIZATION, "Water Affinity", "Water magic effectiveness"),
    EARTH_AFFINITY(9, StatFamily.ELEMENTAL_SPECIALIZATION, "Earth Affinity", "Earth magic effectiveness"),
    FIRE_AFFINITY(10, StatFamily.ELEMENTAL_SPECIALIZATION, "Fire Affinity", "Fire magic effectiveness"),
    AIR_AFFINITY(11, StatFamily.ELEMENTAL_SPECIALIZATION, "Air Affinity", "Air magic effectiveness"),
    MAGIC_RESISTANCE(12, StatFamily.MAGICAL_CORE, "Magic Resistance", "Magic damage reduction"),
    CASTING_SPEED(13, StatFamily.MAGICAL_CORE, "Casting Speed", "Faster spell casting"),
    MANA_POOL(14, StatFamily.MAGICAL_CORE, "Mana Pool", "Maximum mana"),
    ERUDITION(15, StatFamily.MAGICAL_CORE, "Erudition", "Spell variety and learning"),
    TRACKING(16, StatFamily.RANGED_HUNT_CONTROL, "Tracking", "Mob detection and marking"),
    KEEN_SENSES(17, StatFamily.RANGED_HUNT_CONTROL, "Keen Senses", "Dodge and perception"),
    FORGING(18, StatFamily.CRAFTING_SUPPORT, "Forging", "Tool and weapon repair"),
    COOKING(19, StatFamily.CRAFTING_SUPPORT, "Cooking", "Food saturation"),
    ALCHEMY(20, StatFamily.CRAFTING_SUPPORT, "Alchemy", "Potion duration"),
    INTIMIDATION(21, StatFamily.MENTAL_PRESSURE_RESILIENCE, "Intimidation", "Bonus damage to marked targets"),
    WILLPOWER(22, StatFamily.MENTAL_PRESSURE_RESILIENCE, "Willpower", "Status effect resistance");

    public final int index;
    public final StatFamily family;
    public final String displayName;
    public final String description;

    StatType(int index, StatFamily family, String displayName, String description) {
        this.index = index;
        this.family = family;
        this.displayName = displayName;
        this.description = description;
    }

    public StatFamily family() {
        return family;
    }

    public boolean hasPerks() {
        return true;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.StatTypeTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/stats/StatFamily.java src/main/java/tong/statmod/stats/StatType.java src/test/java/tong/statmod/StatTypeTest.java
git commit -m "Add stat families and enable magical perk stats"
```

### Task 2: Extend `Perk` With Magical Core and Elemental Perks

**Files:**
- Modify: `src/main/java/tong/statmod/perks/Perk.java`
- Create: `src/test/java/tong/statmod/perks/MagicPerkCatalogTest.java`
- Modify: `src/test/java/tong/statmod/PerkManagerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.perks;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class MagicPerkCatalogTest {
    @Test
    void everyMagicalStatGetsATierChain() {
        assertEquals(Perk.ARCANE_CORE, Perk.byStatAndTier(StatType.ARCANE_POWER, PerkTier.CORE));
        assertEquals(Perk.WATER_MASTERY, Perk.byStatAndTier(StatType.WATER_AFFINITY, PerkTier.MASTERY));
        assertEquals(Perk.AIR_TRANSCENDENCE, Perk.byStatAndTier(StatType.AIR_AFFINITY, PerkTier.TRANSCENDENCE));
        assertEquals(Perk.ERUDITION_ACTIVE, Perk.byStatAndTier(StatType.ERUDITION, PerkTier.ACTIVE));
    }
}
```

```java
@Test
void magicalPerksUnlockThroughCanonicalPointChecks() {
    PlayerStatData data = new PlayerStatData();
    data.setLevel(StatType.ARCANE_POWER.index, 10);
    data.setPerkPoints(StatType.ARCANE_POWER.index, 1);
    PerkManager mgr = new PerkManager(data);

    assertTrue(mgr.canUnlock(Perk.ARCANE_CORE));
    assertTrue(mgr.unlock(Perk.ARCANE_CORE));
    assertTrue(data.isPerkUnlocked(Perk.ARCANE_CORE.id));
    assertEquals(0, data.getPerkPointsForStat(StatType.ARCANE_POWER.index));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.perks.MagicPerkCatalogTest --tests tong.statmod.PerkManagerTest`  
Expected: FAIL because the magical perk constants do not exist

- [ ] **Step 3: Write minimal implementation**

Append the following magical entries to `Perk.java` after `WILL_TRANSCENDENCE`:

```java
    ARCANE_CORE(84, StatType.ARCANE_POWER, PerkTier.CORE, "Spell Pressure", "+5% offensive magic potency"),
    ARCANE_ACTIVE(85, StatType.ARCANE_POWER, PerkTier.ACTIVE, "Arc Burst", "Short offensive magic burst after a clean cast"),
    ARCANE_SYNERGY(86, StatType.ARCANE_POWER, PerkTier.SYNERGY, "Overchannel", "Synergy: offensive spells gain pressure when cast quickly", StatType.CASTING_SPEED),
    ARCANE_SITUATIONAL(87, StatType.ARCANE_POWER, PerkTier.SITUATIONAL, "Spellbreaker", "Bonus damage against staggered or pinned targets"),
    ARCANE_MASTERY(88, StatType.ARCANE_POWER, PerkTier.MASTERY, "Arcane Cascade", "Offensive casts chain pressure into the next spell"),
    ARCANE_TRANSCENDENCE(89, StatType.ARCANE_POWER, PerkTier.TRANSCENDENCE, "Cataclysm Engine", "Large offensive magic spike windows"),

    WATER_CORE(90, StatType.WATER_AFFINITY, PerkTier.CORE, "Soothing Current", "Water spells restore stability more effectively"),
    WATER_ACTIVE(91, StatType.WATER_AFFINITY, PerkTier.ACTIVE, "Healing Surge", "Water casts briefly improve recovery"),
    WATER_SYNERGY(92, StatType.WATER_AFFINITY, PerkTier.SYNERGY, "Reservoir Flow", "Synergy: water magic scales with deep reserves", StatType.MANA_POOL),
    WATER_SITUATIONAL(93, StatType.WATER_AFFINITY, PerkTier.SITUATIONAL, "Cold Veil", "Defensive water magic improves under pressure"),
    WATER_MASTERY(94, StatType.WATER_AFFINITY, PerkTier.MASTERY, "Tidal Control", "Water casts apply stronger adaptive control"),
    WATER_TRANSCENDENCE(95, StatType.WATER_AFFINITY, PerkTier.TRANSCENDENCE, "Abyssal Grace", "Water magic becomes an elite sustain school"),

    EARTH_CORE(96, StatType.EARTH_AFFINITY, PerkTier.CORE, "Stone Skin", "Earth spells reinforce structure and protection"),
    EARTH_ACTIVE(97, StatType.EARTH_AFFINITY, PerkTier.ACTIVE, "Earthen Rampart", "Barrier spells gain a stronger first layer"),
    EARTH_SYNERGY(98, StatType.EARTH_AFFINITY, PerkTier.SYNERGY, "Runic Bedrock", "Synergy: earth defenses harden against hostile magic", StatType.MAGIC_RESISTANCE),
    EARTH_SITUATIONAL(99, StatType.EARTH_AFFINITY, PerkTier.SITUATIONAL, "Gravity Well", "Earth control is stronger against committed enemies"),
    EARTH_MASTERY(100, StatType.EARTH_AFFINITY, PerkTier.MASTERY, "World Anchor", "Earth magic anchors the caster and the field"),
    EARTH_TRANSCENDENCE(101, StatType.EARTH_AFFINITY, PerkTier.TRANSCENDENCE, "Mountain Throne", "Earth magic becomes a dominant control shell"),

    FIRE_CORE(102, StatType.FIRE_AFFINITY, PerkTier.CORE, "Kindling", "Fire spells burn harder"),
    FIRE_ACTIVE(103, StatType.FIRE_AFFINITY, PerkTier.ACTIVE, "Flashburn", "First offensive fire cast after setup hits harder"),
    FIRE_SYNERGY(104, StatType.FIRE_AFFINITY, PerkTier.SYNERGY, "Accelerant", "Synergy: fire gains pressure from fast casting", StatType.CASTING_SPEED),
    FIRE_SITUATIONAL(105, StatType.FIRE_AFFINITY, PerkTier.SITUATIONAL, "Execution Flame", "Fire punishes weakened targets"),
    FIRE_MASTERY(106, StatType.FIRE_AFFINITY, PerkTier.MASTERY, "Inferno Spiral", "Fire spell chains become more explosive"),
    FIRE_TRANSCENDENCE(107, StatType.FIRE_AFFINITY, PerkTier.TRANSCENDENCE, "Solar Cataclysm", "Fire becomes the peak offensive element"),

    AIR_CORE(108, StatType.AIR_AFFINITY, PerkTier.CORE, "Tailwind", "Air spells improve movement-oriented casting"),
    AIR_ACTIVE(109, StatType.AIR_AFFINITY, PerkTier.ACTIVE, "Gale Step", "Air casts improve repositioning windows"),
    AIR_SYNERGY(110, StatType.AIR_AFFINITY, PerkTier.SYNERGY, "Sky Dancer", "Synergy: air magic rewards mobile bodies", StatType.AGILITY),
    AIR_SITUATIONAL(111, StatType.AIR_AFFINITY, PerkTier.SITUATIONAL, "Storm Reach", "Air pressure extends on displaced targets"),
    AIR_MASTERY(112, StatType.AIR_AFFINITY, PerkTier.MASTERY, "Lightning Thread", "Fast air casts weave through combat windows"),
    AIR_TRANSCENDENCE(113, StatType.AIR_AFFINITY, PerkTier.TRANSCENDENCE, "Tempest Crown", "Air becomes the supreme mobility element"),

    MAGIC_RESIST_CORE(114, StatType.MAGIC_RESISTANCE, PerkTier.CORE, "Warding Skin", "Hostile magic is slightly blunted"),
    MAGIC_RESIST_ACTIVE(115, StatType.MAGIC_RESISTANCE, PerkTier.ACTIVE, "Spell Shear", "Clean defense shaves pressure off incoming magic"),
    MAGIC_RESIST_SYNERGY(116, StatType.MAGIC_RESISTANCE, PerkTier.SYNERGY, "Unbroken Ward", "Synergy: magical defense hardens with mental discipline", StatType.WILLPOWER),
    MAGIC_RESIST_SITUATIONAL(117, StatType.MAGIC_RESISTANCE, PerkTier.SITUATIONAL, "Countercurrent", "Magic defense spikes under caster pressure"),
    MAGIC_RESIST_MASTERY(118, StatType.MAGIC_RESISTANCE, PerkTier.MASTERY, "Null Mantle", "Advanced hostile spell effects lose efficiency"),
    MAGIC_RESIST_TRANSCENDENCE(119, StatType.MAGIC_RESISTANCE, PerkTier.TRANSCENDENCE, "Aegis Absolute", "Elite anti-magic posture"),

    CASTING_SPEED_CORE(120, StatType.CASTING_SPEED, PerkTier.CORE, "Quick Sigils", "Basic casting flow is cleaner"),
    CASTING_SPEED_ACTIVE(121, StatType.CASTING_SPEED, PerkTier.ACTIVE, "Snapcast", "One rapid cast window after stable setup"),
    CASTING_SPEED_SYNERGY(122, StatType.CASTING_SPEED, PerkTier.SYNERGY, "Pressure Casting", "Synergy: fast execution empowers offensive spells", StatType.ARCANE_POWER),
    CASTING_SPEED_SITUATIONAL(123, StatType.CASTING_SPEED, PerkTier.SITUATIONAL, "Window Theft", "Fast casts punish short openings better"),
    CASTING_SPEED_MASTERY(124, StatType.CASTING_SPEED, PerkTier.MASTERY, "Spell Weave", "Spell strings become exceptionally fluid"),
    CASTING_SPEED_TRANSCENDENCE(125, StatType.CASTING_SPEED, PerkTier.TRANSCENDENCE, "Timeless Cast", "Extreme casting tempo expression"),

    MANA_POOL_CORE(126, StatType.MANA_POOL, PerkTier.CORE, "Deep Wells", "Maximum mana slightly increases"),
    MANA_POOL_ACTIVE(127, StatType.MANA_POOL, PerkTier.ACTIVE, "Mana Draw", "Short reserve recovery after disciplined pacing"),
    MANA_POOL_SYNERGY(128, StatType.MANA_POOL, PerkTier.SYNERGY, "Disciplined Reserve", "Synergy: deeper reserves reward learned casting", StatType.ERUDITION),
    MANA_POOL_SITUATIONAL(129, StatType.MANA_POOL, PerkTier.SITUATIONAL, "Last Reservoir", "Low-reserve casting degrades more slowly"),
    MANA_POOL_MASTERY(130, StatType.MANA_POOL, PerkTier.MASTERY, "Endless Cycle", "Long-form casting becomes steadier"),
    MANA_POOL_TRANSCENDENCE(131, StatType.MANA_POOL, PerkTier.TRANSCENDENCE, "Ocean Soul", "Peak mana endurance"),

    ERUDITION_CORE(132, StatType.ERUDITION, PerkTier.CORE, "Scholar's Eye", "Learned magic reveals more structure"),
    ERUDITION_ACTIVE(133, StatType.ERUDITION, PerkTier.ACTIVE, "Pattern Recall", "Recently used spell patterns become easier to repeat"),
    ERUDITION_SYNERGY(134, StatType.ERUDITION, PerkTier.SYNERGY, "Focused Thesis", "Synergy: disciplined minds stabilize complex magic", StatType.WILLPOWER),
    ERUDITION_SITUATIONAL(135, StatType.ERUDITION, PerkTier.SITUATIONAL, "Adaptive Theory", "Flexible casters pivot more efficiently"),
    ERUDITION_MASTERY(136, StatType.ERUDITION, PerkTier.MASTERY, "Grand Synthesis", "Multi-school usage becomes cleaner"),
    ERUDITION_TRANSCENDENCE(137, StatType.ERUDITION, PerkTier.TRANSCENDENCE, "Omniform Understanding", "Top-end magical mastery");
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.perks.MagicPerkCatalogTest --tests tong.statmod.PerkManagerTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/perks/Perk.java src/test/java/tong/statmod/perks/MagicPerkCatalogTest.java src/test/java/tong/statmod/PerkManagerTest.java
git commit -m "Add magical perk chains to canonical perk catalog"
```

### Task 3: Add Canonical Tensura Spell Taxonomy

**Files:**
- Create: `src/main/java/tong/statmod/integration/tensura/TensuraSpellProfile.java`
- Create: `src/main/java/tong/statmod/integration/tensura/TensuraSpellTaxonomy.java`
- Modify: `src/main/java/tong/statmod/integration/tensura/TensuraSpellGate.java`
- Create: `src/test/java/tong/statmod/integration/tensura/TensuraSpellTaxonomyTest.java`
- Modify: `src/test/java/tong/statmod/integration/tensura/TensuraSpellGateTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class TensuraSpellTaxonomyTest {
    @Test
    void classifiesCurrentCanonicalTensuraSpells() {
        assertEquals(StatType.FIRE_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:fire_bolt"));
        assertEquals(StatType.WATER_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:healing_rain"));
        assertEquals(StatType.EARTH_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:earth_barrier"));
        assertEquals(StatType.AIR_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:wind_cutter"));
        assertEquals(StatType.ERUDITION, TensuraSpellTaxonomy.primaryStat("tensura:spatial_movement"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.tensura.TensuraSpellTaxonomyTest`  
Expected: FAIL because `TensuraSpellTaxonomy` does not exist

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.tensura;

import tong.statmod.stats.StatType;

import java.util.List;

public record TensuraSpellProfile(
        String skillId,
        String discipline,
        StatType primaryStat,
        List<StatType> secondaryStats,
        boolean elemental
) {}
```

```java
package tong.statmod.integration.tensura;

import tong.statmod.stats.StatType;

import java.util.List;
import java.util.Map;

public final class TensuraSpellTaxonomy {
    private static final Map<String, TensuraSpellProfile> PROFILES = Map.of(
            "tensura:fire_bolt", new TensuraSpellProfile("tensura:fire_bolt", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER), true),
            "tensura:healing_rain", new TensuraSpellProfile("tensura:healing_rain", "support", StatType.WATER_AFFINITY, List.of(StatType.MANA_POOL, StatType.ERUDITION), true),
            "tensura:earth_barrier", new TensuraSpellProfile("tensura:earth_barrier", "defense", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE), true),
            "tensura:wind_cutter", new TensuraSpellProfile("tensura:wind_cutter", "offense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED), true),
            "tensura:light_binding", new TensuraSpellProfile("tensura:light_binding", "control", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER), false),
            "tensura:darkness", new TensuraSpellProfile("tensura:darkness", "pressure", StatType.ARCANE_POWER, List.of(StatType.INTIMIDATION, StatType.WILLPOWER), false),
            "tensura:spatial_movement", new TensuraSpellProfile("tensura:spatial_movement", "mobility", StatType.ERUDITION, List.of(StatType.CASTING_SPEED, StatType.AIR_AFFINITY), false)
    );

    private TensuraSpellTaxonomy() {}

    public static TensuraSpellProfile profile(String skillId) {
        return PROFILES.get(TensuraSkillIds.canonicalize(skillId));
    }

    public static StatType primaryStat(String skillId) {
        TensuraSpellProfile profile = profile(skillId);
        return profile == null ? StatType.ARCANE_POWER : profile.primaryStat();
    }
}
```

Update `TensuraSpellGate.resolveTensuraSpellId` and `resolveForPerk` to prefer taxonomy-backed IDs before generic stat fallback:

```java
public static String resolveTensuraSpellId(String perkKey) {
    return TensuraSkillIds.canonicalize(PERK_TO_TENSURA.get(perkKey));
}

public static StatType primaryStatForResolvedSkill(String skillId) {
    return TensuraSpellTaxonomy.primaryStat(skillId);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.tensura.TensuraSpellTaxonomyTest --tests tong.statmod.integration.tensura.TensuraSpellGateTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/tensura/TensuraSpellProfile.java src/main/java/tong/statmod/integration/tensura/TensuraSpellTaxonomy.java src/main/java/tong/statmod/integration/tensura/TensuraSpellGate.java src/test/java/tong/statmod/integration/tensura/TensuraSpellTaxonomyTest.java src/test/java/tong/statmod/integration/tensura/TensuraSpellGateTest.java
git commit -m "Add canonical Tensura spell taxonomy"
```

### Task 4: Add Canonical Mahou Spell Taxonomy and Replace Loose Element Routing

**Files:**
- Create: `src/main/java/tong/statmod/integration/mahou/MahouSpellProfile.java`
- Create: `src/main/java/tong/statmod/integration/mahou/MahouSpellTaxonomy.java`
- Modify: `src/main/java/tong/statmod/integration/mahou/MahouElementMapper.java`
- Modify: `src/main/java/tong/statmod/integration/mahou/MahouPerkMap.java`
- Create: `src/test/java/tong/statmod/integration/mahou/MahouSpellTaxonomyTest.java`
- Modify: `src/test/java/tong/statmod/integration/mahou/MahouMappingTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.mahou;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MahouSpellTaxonomyTest {
    @Test
    void classifiesCuratedMahouSignatureSpells() {
        assertEquals(StatType.ARCANE_POWER, MahouSpellTaxonomy.primaryStat("mahoutsukai:gandr_spell_scroll"));
        assertEquals(StatType.EARTH_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:rho_aias_spell_scroll"));
        assertEquals(StatType.FIRE_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:fallen_down_spell_scroll"));
        assertEquals(StatType.ERUDITION, MahouSpellTaxonomy.primaryStat("mahoutsukai:mystic_staff_spell_scroll"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.mahou.MahouSpellTaxonomyTest`  
Expected: FAIL because `MahouSpellTaxonomy` does not exist

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.mahou;

import tong.statmod.stats.StatType;

import java.util.List;

public record MahouSpellProfile(
        String itemId,
        String family,
        StatType primaryStat,
        List<StatType> secondaryStats
) {}
```

```java
package tong.statmod.integration.mahou;

import tong.statmod.stats.StatType;

import java.util.List;
import java.util.Map;

public final class MahouSpellTaxonomy {
    private static final Map<String, MahouSpellProfile> PROFILES = Map.of(
            "mahoutsukai:gandr_spell_scroll", new MahouSpellProfile("mahoutsukai:gandr_spell_scroll", "arcane_offense", StatType.ARCANE_POWER, List.of(StatType.CASTING_SPEED)),
            "mahoutsukai:rho_aias_spell_scroll", new MahouSpellProfile("mahoutsukai:rho_aias_spell_scroll", "barrier", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE, StatType.WILLPOWER)),
            "mahoutsukai:fallen_down_spell_scroll", new MahouSpellProfile("mahoutsukai:fallen_down_spell_scroll", "cataclysm", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER)),
            "mahoutsukai:mystic_staff_spell_scroll", new MahouSpellProfile("mahoutsukai:mystic_staff_spell_scroll", "mastery", StatType.ERUDITION, List.of(StatType.MANA_POOL))
    );

    private MahouSpellTaxonomy() {}

    public static MahouSpellProfile profile(String itemId) {
        return PROFILES.get(itemId);
    }

    public static StatType primaryStat(String itemId) {
        MahouSpellProfile profile = profile(itemId);
        return profile == null ? StatType.ARCANE_POWER : profile.primaryStat();
    }
}
```

Update `MahouElementMapper` and `MahouPerkMap` to prefer curated taxonomy:

```java
public static String elementForPath(String path) {
    String fullId = path != null && path.contains(":") ? path : "mahoutsukai:" + path;
    MahouSpellProfile profile = MahouSpellTaxonomy.profile(fullId);
    if (profile != null) {
        return switch (profile.primaryStat()) {
            case FIRE_AFFINITY -> "fire";
            case WATER_AFFINITY -> "water";
            case EARTH_AFFINITY -> "earth";
            case AIR_AFFINITY -> "air";
            default -> "arcane";
        };
    }
    // keep existing fallback heuristic below
```

```java
public static StatType[] statsForElement(String element) {
    return switch (element) {
        case "fire" -> new StatType[]{StatType.FIRE_AFFINITY, StatType.ARCANE_POWER};
        case "water" -> new StatType[]{StatType.WATER_AFFINITY, StatType.MANA_POOL};
        case "earth" -> new StatType[]{StatType.EARTH_AFFINITY, StatType.MAGIC_RESISTANCE};
        case "air" -> new StatType[]{StatType.AIR_AFFINITY, StatType.CASTING_SPEED};
        default -> new StatType[]{StatType.ARCANE_POWER, StatType.ERUDITION};
    };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.mahou.MahouSpellTaxonomyTest --tests tong.statmod.integration.mahou.MahouMappingTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/mahou/MahouSpellProfile.java src/main/java/tong/statmod/integration/mahou/MahouSpellTaxonomy.java src/main/java/tong/statmod/integration/mahou/MahouElementMapper.java src/main/java/tong/statmod/integration/mahou/MahouPerkMap.java src/test/java/tong/statmod/integration/mahou/MahouSpellTaxonomyTest.java src/test/java/tong/statmod/integration/mahou/MahouMappingTest.java
git commit -m "Add canonical Mahou spell taxonomy"
```

### Task 5: Introduce Family-Based Puffish Category Mapping and Family Totals

**Files:**
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilder.java`
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java`
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java`
- Create: `src/test/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilderTest.java`
- Modify: `src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java`
- Modify: `src/test/java/tong/statmod/integration/puffish/PuffishUnlockServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishFamilyTreeBuilderTest {
    @Test
    void mapsPerksIntoFamilyCategories() {
        assertEquals("statmod:frontline_physical_combat", PuffishPerkIds.categoryId(Perk.BRUTE_CORE));
        assertEquals("statmod:magical_core", PuffishPerkIds.categoryId(Perk.ARCANE_CORE));
        assertEquals("arcane_power__arcane_core", PuffishPerkIds.skillId(Perk.ARCANE_CORE));
    }

    @Test
    void configUsesSixFamilyTabs() {
        assertTrue(PuffishFamilyTreeBuilder.configJson().contains("\"magical_core\""));
        assertTrue(PuffishFamilyTreeBuilder.configJson().contains("\"elemental_specialization\""));
    }
}
```

Add to `PuffishSyncServiceTest`:

```java
@Test
void mirrorsFamilyTotalsInsteadOfStatCategoryTotals() {
    PlayerStatData data = new PlayerStatData();
    data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 3);
    data.setPerkPoints(Perk.BLADE_CORE.stat.index, 2);

    FakeGateway gateway = new FakeGateway();
    PuffishSyncService.sync(data, gateway);

    assertTrue(gateway.operations.contains("points:statmod:frontline_physical_combat:5"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishFamilyTreeBuilderTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest --tests tong.statmod.integration.puffish.PuffishUnlockServiceTest`  
Expected: FAIL because category ids still point at individual stat tabs

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.stats.StatFamily;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class PuffishFamilyTreeBuilder {
    private PuffishFamilyTreeBuilder() {}

    public static String configJson() {
        String categories = Arrays.stream(StatFamily.values())
                .map(StatFamily::slug)
                .map(slug -> "        \"" + slug + "\"")
                .collect(Collectors.joining(",\n"));
        return "{\n    \"version\": 3,\n    \"categories\": [\n" + categories + "\n    ]\n}\n";
    }

    public static String categoryId(Perk perk) {
        return "statmod:" + perk.stat.family().slug;
    }

    public static String skillId(Perk perk) {
        return perk.stat.name().toLowerCase(Locale.ROOT) + "__" + perk.name().toLowerCase(Locale.ROOT);
    }
}
```

Update `PuffishPerkIds`:

```java
public static String categoryId(Perk perk) {
    return PuffishFamilyTreeBuilder.categoryId(perk);
}

public static String skillId(Perk perk) {
    return PuffishFamilyTreeBuilder.skillId(perk);
}
```

Update `PuffishSyncService`:

```java
public static void sync(PlayerStatData data, PuffishMirrorGateway gateway) {
    Set<String> initializedCategories = new HashSet<>();
    for (Perk perk : Perk.values()) {
        String categoryId = PuffishPerkIds.categoryId(perk);
        if (initializedCategories.add(categoryId)) {
            gateway.ensureCategoryUnlocked(categoryId);
            gateway.setPoints(categoryId, mirroredFamilyTotal(data, perk.stat.family()));
        }
        if (data.isPerkUnlocked(perk.id)) {
            gateway.unlock(categoryId, PuffishPerkIds.skillId(perk));
        } else {
            gateway.lock(categoryId, PuffishPerkIds.skillId(perk));
        }
    }
}

private static int mirroredFamilyTotal(PlayerStatData data, StatFamily family) {
    int total = 0;
    for (StatType stat : StatType.values()) {
        if (stat.family() == family) {
            total += data.getPerkPointsForStat(stat.index);
        }
    }
    for (Perk perk : Perk.values()) {
        if (perk.stat.family() == family && data.isPerkUnlocked(perk.id)) {
            total += perk.tier.cost;
        }
    }
    return total;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishFamilyTreeBuilderTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest --tests tong.statmod.integration.puffish.PuffishUnlockServiceTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilder.java src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java src/test/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilderTest.java src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java src/test/java/tong/statmod/integration/puffish/PuffishUnlockServiceTest.java
git commit -m "Map Puffish trees and sync to stat families"
```

### Task 6: Export and Check In Family-Tab Puffish Resources

**Files:**
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeExporter.java`
- Modify: `src/main/resources/data/statmod/puffish_skills/config.json`
- Modify: `src/main/resources/data/statmod/puffish_skills/categories/**`
- Create: `src/test/java/tong/statmod/integration/puffish/PuffishFamilyResourceConfigTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishFamilyResourceConfigTest {
    @Test
    void checkedInConfigUsesFamilyTabs() throws Exception {
        try (InputStream stream = PuffishFamilyResourceConfigTest.class.getClassLoader()
                .getResourceAsStream("data/statmod/puffish_skills/config.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"frontline_physical_combat\""));
            assertTrue(json.contains("\"magical_core\""));
            assertTrue(json.contains("\"elemental_specialization\""));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishFamilyResourceConfigTest`  
Expected: FAIL because the checked-in `config.json` still lists per-stat categories

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.puffish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PuffishFamilyTreeExporter {
    private PuffishFamilyTreeExporter() {}

    public static void main(String[] args) throws IOException {
        Path root = Path.of("src/main/resources/data/statmod/puffish_skills");
        Files.createDirectories(root);
        Files.writeString(root.resolve("config.json"), PuffishFamilyTreeBuilder.configJson());
    }
}
```

Replace `src/main/resources/data/statmod/puffish_skills/config.json` with:

```json
{
    "version": 3,
    "categories": [
        "frontline_physical_combat",
        "ranged_hunt_control",
        "magical_core",
        "elemental_specialization",
        "mental_pressure_resilience",
        "crafting_support"
    ]
}
```

Then run the exporter once and check in the generated family category directories under `src/main/resources/data/statmod/puffish_skills/categories/`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishFamilyResourceConfigTest --tests tong.statmod.integration.puffish.PuffishOverrideResourceTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeExporter.java src/main/resources/data/statmod/puffish_skills src/test/java/tong/statmod/integration/puffish/PuffishFamilyResourceConfigTest.java
git commit -m "Check in family-based Puffish tree resources"
```

### Task 7: Final Verification for the Foundation Slice

**Files:**
- Verify only

- [ ] **Step 1: Run focused regression suite**

Run:

```bash
.\gradlew.bat test --tests tong.statmod.StatTypeTest --tests tong.statmod.perks.MagicPerkCatalogTest --tests tong.statmod.PerkManagerTest --tests tong.statmod.integration.tensura.TensuraSpellTaxonomyTest --tests tong.statmod.integration.mahou.MahouSpellTaxonomyTest --tests tong.statmod.integration.puffish.PuffishFamilyTreeBuilderTest --tests tong.statmod.integration.puffish.PuffishFamilyResourceConfigTest
```

Expected: PASS

- [ ] **Step 2: Run full build**

Run: `.\gradlew.bat build`  
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run client smoke test**

Run: `.\gradlew.bat runClient`  
Expected:

- Puffish opens with family tabs
- magical families appear in the tree
- existing physical perks still load
- no statmod crash from perk id remapping
- no sync crash when opening the family-based tree

- [ ] **Step 4: Commit final integration checkpoint**

```bash
git add -A
git commit -m "Finish stat family perk foundation"
```

---

## Self-Review

### Spec coverage

- magical stats get perks: Task 2
- family-organized perk tabs: Task 5, Task 6
- mana-only magical resource direction: Task 3, Task 4 preserve magic-only taxonomy and do not route spells through stamina
- Tensura spell understanding first: Task 3
- Mahou understanding second: Task 4
- canonical STAT Mod ownership: Task 1, Task 5

### Placeholder scan

- no `TODO`
- no `TBD`
- no “same as previous task”
- each task has explicit files, tests, commands, and commit points

### Type consistency

- `StatFamily` is introduced before use in `StatType`, `PuffishFamilyTreeBuilder`, and sync code
- magical perks use the same `PerkTier` and `PerkManager` flow as existing perks
- taxonomy classes use `StatType` as the canonical output for both magic mods
