# Phase 1 — Mob Stats System + L2Hostility Bridge

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Donner les 23 stats STAT Mod à tous les mobs Minecraft, piloter leurs niveaux via L2Hostility si présent, et faire interagir les stats joueur avec les traits L2H.

**Architecture:** Nouveau `MobStats` Forge capability attaché à tout `Mob`. Sans L2H : niveaux lus depuis `data/statmod/mob_stats/*.json`. Avec L2H : niveaux calculés depuis `MobTraitCap.lv` + traits actifs. `L2HostilityMobSync` est séparé dans `integration/` et uniquement chargé si L2H est présent (`ModList.isLoaded`).

**Tech Stack:** Java 17, Forge 1.20.1, Forge Capabilities, `SimpleJsonResourceReloadListener`, L2Hostility API (optionnel compileOnly)

**Prérequis :** Phase 0 (correctifs) terminée.

---

## Fichiers à créer / modifier

### Nouveaux fichiers
| Fichier | Rôle |
|---------|------|
| `src/main/java/tong/statmod/capability/MobStats.java` | Données : 23 niveaux int[], NBT sérialisation |
| `src/main/java/tong/statmod/capability/MobStatsProvider.java` | Forge ICapabilityProvider |
| `src/main/java/tong/statmod/stats/MobStatCalculator.java` | Formules : niveau → bonus attribut mob |
| `src/main/java/tong/statmod/stats/MobStatEffectApplier.java` | Applique les bonus sur les attributs du mob |
| `src/main/java/tong/statmod/stats/MobStatInitializer.java` | Lit les JSON et init les stats du mob |
| `src/main/java/tong/statmod/reload/MobStatReloadListener.java` | `SimpleJsonResourceReloadListener` pour mob_stats/ |
| `src/main/java/tong/statmod/integration/L2HostilityMobSync.java` | Bridge L2H → MobStats |
| `src/main/java/tong/statmod/combat/L2HPlayerResistance.java` | Stats joueur résistent aux traits L2H |
| `src/test/java/tong/statmod/capability/MobStatsTest.java` | Tests NBT + sanitize |
| `src/test/java/tong/statmod/stats/MobStatCalculatorTest.java` | Tests formules |
| `src/test/java/tong/statmod/stats/MobStatInitializerTest.java` | Tests calcul niveaux |

### Fichiers JSON à créer
```
src/main/resources/data/statmod/mob_stats/
├── minecraft/zombie.json
├── minecraft/skeleton.json
├── minecraft/creeper.json
├── minecraft/spider.json
├── minecraft/enderman.json
├── minecraft/witch.json
├── minecraft/blaze.json
├── minecraft/wither_skeleton.json
├── minecraft/piglin.json
├── minecraft/pillager.json
├── minecraft/evoker.json
├── minecraft/wither.json
└── minecraft/elder_guardian.json
```

### Fichiers à modifier
| Fichier | Modification |
|---------|-------------|
| `src/main/java/tong/statmod/capability/CapabilityHandler.java` | Attacher MobStats à Mob entities + register |
| `src/main/java/tong/statmod/Config.java` | Ajouter section [mobStats] |
| `src/main/java/tong/statmod/STATMod.java` | Enregistrer MobStatReloadListener + L2H compat init |
| `src/main/java/tong/statmod/progression/CombatXPHandler.java` | Multiplier XP par niveau L2H mob |
| `src/main/resources/META-INF/mods.toml` | Ajouter dépendance optionnelle l2hostility |
| `src/main/java/tong/statmod/ARCHITECTURE.md` | Mise à jour |

---

## Task 1 : MobStats capability — données et NBT

**Fichiers :**
- Créer : `src/main/java/tong/statmod/capability/MobStats.java`
- Créer : `src/test/java/tong/statmod/capability/MobStatsTest.java`

- [ ] **Step 1.1 : Créer le test**

Créer `src/test/java/tong/statmod/capability/MobStatsTest.java` :

```java
package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobStatsTest {

    @Test
    void defaultLevelsAreZero() {
        MobStats stats = new MobStats();
        for (int i = 0; i < MobStats.STAT_COUNT; i++) {
            assertEquals(0, stats.getLevel(i));
        }
    }

    @Test
    void setLevel_clampsAt100() {
        MobStats stats = new MobStats();
        stats.setLevel(0, 150);
        assertEquals(100, stats.getLevel(0));
    }

    @Test
    void setLevel_clampsAtZero() {
        MobStats stats = new MobStats();
        stats.setLevel(0, -5);
        assertEquals(0, stats.getLevel(0));
    }

    @Test
    void nbtRoundTrip_preservesAllLevels() {
        MobStats original = new MobStats();
        for (int i = 0; i < MobStats.STAT_COUNT; i++) {
            original.setLevel(i, i * 4); // 0,4,8,...,88
        }
        CompoundTag tag = original.serializeNBT();

        MobStats loaded = new MobStats();
        loaded.deserializeNBT(tag);

        for (int i = 0; i < MobStats.STAT_COUNT; i++) {
            assertEquals(original.getLevel(i), loaded.getLevel(i),
                "Stat index " + i + " must survive NBT round-trip");
        }
    }

    @Test
    void deserializeNBT_sanitizesCorruptedValues() {
        CompoundTag tag = new CompoundTag();
        int[] badValues = new int[MobStats.STAT_COUNT];
        badValues[0] = 999;   // trop haut
        badValues[1] = -10;   // négatif
        tag.putIntArray("MobStatLevels", badValues);

        MobStats stats = new MobStats();
        stats.deserializeNBT(tag);

        assertEquals(100, stats.getLevel(0), "999 doit être clampé à 100");
        assertEquals(0,   stats.getLevel(1), "-10 doit être clampé à 0");
    }
}
```

- [ ] **Step 1.2 : Lancer pour vérifier l'échec**

```
./gradlew test --tests "tong.statmod.capability.MobStatsTest"
```

Résultat attendu : `FAILED` — `MobStats` n'existe pas.

- [ ] **Step 1.3 : Créer `MobStats.java`**

Créer `src/main/java/tong/statmod/capability/MobStats.java` :

```java
package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class MobStats implements INBTSerializable<CompoundTag> {
    public static final int STAT_COUNT = 23;

    private final int[] statLevels = new int[STAT_COUNT];

    public int getLevel(int index) {
        return statLevels[index];
    }

    public void setLevel(int index, int level) {
        if (index < 0 || index >= STAT_COUNT) return;
        statLevels[index] = Math.max(0, Math.min(100, level));
    }

    public void addToLevel(int index, int amount) {
        setLevel(index, getLevel(index) + amount);
    }

    public void reset() {
        java.util.Arrays.fill(statLevels, 0);
    }

    public void sanitize() {
        for (int i = 0; i < STAT_COUNT; i++) {
            statLevels[i] = Math.max(0, Math.min(100, statLevels[i]));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("MobStatLevels", statLevels.clone());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loaded = tag.getIntArray("MobStatLevels");
        if (loaded.length >= STAT_COUNT) {
            System.arraycopy(loaded, 0, statLevels, 0, STAT_COUNT);
        }
        sanitize();
    }
}
```

- [ ] **Step 1.4 : Lancer les tests**

```
./gradlew test --tests "tong.statmod.capability.MobStatsTest"
```

Résultat attendu : `PASSED` (4 tests).

- [ ] **Step 1.5 : Commit**

```bash
git add src/main/java/tong/statmod/capability/MobStats.java \
        src/test/java/tong/statmod/capability/MobStatsTest.java
git commit -m "feat: add MobStats capability data class (23 stats, NBT serialization)"
```

---

## Task 2 : MobStatsProvider — Forge capability provider

**Fichiers :**
- Créer : `src/main/java/tong/statmod/capability/MobStatsProvider.java`

- [ ] **Step 2.1 : Créer `MobStatsProvider.java`**

```java
package tong.statmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobStatsProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<MobStats> MOB_STATS =
        CapabilityManager.get(new CapabilityToken<>() {});

    private MobStats instance = null;
    private final LazyOptional<MobStats> optional =
        LazyOptional.of(this::getOrCreate);

    private MobStats getOrCreate() {
        if (instance == null) instance = new MobStats();
        return instance;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
        @NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MOB_STATS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return getOrCreate().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getOrCreate().deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
```

- [ ] **Step 2.2 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 2.3 : Commit**

```bash
git add src/main/java/tong/statmod/capability/MobStatsProvider.java
git commit -m "feat: add MobStatsProvider (Forge capability provider for mobs)"
```

---

## Task 3 : MobStatCalculator — formules niveau → bonus

**Fichiers :**
- Créer : `src/main/java/tong/statmod/stats/MobStatCalculator.java`
- Créer : `src/test/java/tong/statmod/stats/MobStatCalculatorTest.java`

- [ ] **Step 3.1 : Créer le test**

Créer `src/test/java/tong/statmod/stats/MobStatCalculatorTest.java` :

```java
package tong.statmod.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobStatCalculatorTest {

    @Test
    void getDamageBonusMultiplier_zeroAtLevelZero() {
        assertEquals(0.0f, MobStatCalculator.getDamageBonusMultiplier(0), 0.001f);
    }

    @Test
    void getDamageBonusMultiplier_twoAtLevelHundred() {
        // level 100 → +2.0 (×3 total)
        assertEquals(2.0f, MobStatCalculator.getDamageBonusMultiplier(100), 0.001f);
    }

    @Test
    void getHealthBonusFlat_zeroAtLevelZero() {
        assertEquals(0.0f, MobStatCalculator.getHealthBonusFlat(0), 0.001f);
    }

    @Test
    void getHealthBonusFlat_fiftyAtLevelHundred() {
        // level 100 → +50 HP
        assertEquals(50.0f, MobStatCalculator.getHealthBonusFlat(100), 0.001f);
    }

    @Test
    void getSpeedBonus_zeroAtLevelZero() {
        assertEquals(0.0f, MobStatCalculator.getSpeedBonus(0), 0.001f);
    }

    @Test
    void getDamageReduction_cappedAtHalf() {
        // max 50% reduction à level 100
        float reduction = MobStatCalculator.getDamageReduction(100);
        assertTrue(reduction <= 0.5f, "Damage reduction should not exceed 50%");
    }

    @Test
    void getFollowRangeBonus_growsWithLevel() {
        float low  = MobStatCalculator.getFollowRangeBonus(10);
        float high = MobStatCalculator.getFollowRangeBonus(50);
        assertTrue(high > low);
    }
}
```

- [ ] **Step 3.2 : Lancer pour vérifier l'échec**

```
./gradlew test --tests "tong.statmod.stats.MobStatCalculatorTest"
```

- [ ] **Step 3.3 : Créer `MobStatCalculator.java`**

```java
package tong.statmod.stats;

public class MobStatCalculator {

    /** BRUTE_FORCE : bonus de dégâts multiplicatif. level 100 → +2.0 (×3 total). */
    public static float getDamageBonusMultiplier(int level) {
        return level / 100.0f * 2.0f;
    }

    /** PHYSICAL_ENDURANCE : bonus de PV plat. level 100 → +50 HP. */
    public static float getHealthBonusFlat(int level) {
        return level * 0.5f;
    }

    /** AGILITY + RAPIDITE : bonus de vitesse multiplicatif combiné. */
    public static float getSpeedBonus(int level) {
        return level / 100.0f * 0.3f;
    }

    /** PHYSICAL_RESISTANCE : réduction de dégâts [0, 0.5]. */
    public static float getDamageReduction(int level) {
        return Math.min(level / 200.0f, 0.5f);
    }

    /** TRACKING : bonus de portée de détection en blocs. */
    public static float getFollowRangeBonus(int level) {
        return level * 0.4f;
    }

    /** ARCANE_POWER : multiplicateur de dégâts magiques. */
    public static float getMagicDamageMultiplier(int level) {
        return level / 100.0f * 1.5f;
    }

    /** FIRE/WATER/EARTH/AIR_AFFINITY : résistance élémentaire [0, 0.5]. */
    public static float getElementalResistance(int level) {
        return Math.min(level / 200.0f, 0.5f);
    }

    /** MAGIC_RESISTANCE : réduction de dégâts magiques [0, 0.5]. */
    public static float getMagicResistance(int level) {
        return Math.min(level / 200.0f, 0.5f);
    }

    /** WILLPOWER : résistance au knockback [0, 1]. */
    public static float getKnockbackResistance(int level) {
        return Math.min(level / 100.0f, 1.0f);
    }

    /** INTIMIDATION : rayon d'aura de slowness en blocs. */
    public static float getIntimidationRadius(int level) {
        return level / 20.0f;
    }

    /** MANA_POOL : mana max du mob pour les skills. */
    public static int getMobMaxMana(int level) {
        return level * 2;
    }
}
```

- [ ] **Step 3.4 : Lancer les tests**

```
./gradlew test --tests "tong.statmod.stats.MobStatCalculatorTest"
```

Résultat attendu : `PASSED` (7 tests).

- [ ] **Step 3.5 : Commit**

```bash
git add src/main/java/tong/statmod/stats/MobStatCalculator.java \
        src/test/java/tong/statmod/stats/MobStatCalculatorTest.java
git commit -m "feat: add MobStatCalculator (formulas: level → attribute bonus for mobs)"
```

---

## Task 4 : MobStatEffectApplier — appliquer les bonus sur le mob

**Fichiers :**
- Créer : `src/main/java/tong/statmod/stats/MobStatEffectApplier.java`

- [ ] **Step 4.1 : Créer `MobStatEffectApplier.java`**

```java
package tong.statmod.stats;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;

import java.util.UUID;

public class MobStatEffectApplier {

    private static final UUID MOB_STAT_UUID =
        UUID.fromString("9a8b7c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d");

    public static void applyAllBonuses(Mob mob) {
        mob.getCapability(MobStatsProvider.MOB_STATS).ifPresent(stats -> {
            applyDamage(mob, stats);
            applyHealth(mob, stats);
            applySpeed(mob, stats);
            applyResistance(mob, stats);
            applyFollowRange(mob, stats);
            applyKnockbackResistance(mob, stats);
        });
    }

    private static void applyDamage(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getDamageBonusMultiplier(
            stats.getLevel(StatType.BRUTE_FORCE.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_damage",
                bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void applyHealth(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getHealthBonusFlat(
            stats.getLevel(StatType.PHYSICAL_ENDURANCE.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_health",
                bonus, AttributeModifier.Operation.ADDITION));
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private static void applySpeed(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        int combinedLevel = stats.getLevel(StatType.AGILITY.index)
                          + stats.getLevel(StatType.RAPIDITE.index);
        float bonus = MobStatCalculator.getSpeedBonus(combinedLevel / 2);
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_speed",
                bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void applyResistance(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.ARMOR);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float reduction = MobStatCalculator.getDamageReduction(
            stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
        if (reduction > 0) {
            // Convertit la réduction en armor (approximation)
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_armor",
                reduction * 20.0, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void applyFollowRange(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getFollowRangeBonus(
            stats.getLevel(StatType.TRACKING.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_follow_range",
                bonus, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void applyKnockbackResistance(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getKnockbackResistance(
            stats.getLevel(StatType.WILLPOWER.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_knockback_res",
                bonus, AttributeModifier.Operation.ADDITION));
        }
    }
}
```

- [ ] **Step 4.2 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 4.3 : Commit**

```bash
git add src/main/java/tong/statmod/stats/MobStatEffectApplier.java
git commit -m "feat: add MobStatEffectApplier (applies MobStats bonuses to Mob attributes)"
```

---

## Task 5 : JSON mob_stats + MobStatReloadListener

**Fichiers :**
- Créer : `src/main/java/tong/statmod/reload/MobStatReloadListener.java`
- Créer : `src/main/java/tong/statmod/stats/MobStatInitializer.java`
- Créer : les 13 fichiers JSON vanilla

- [ ] **Step 5.1 : Créer les JSON vanilla**

Créer `src/main/resources/data/statmod/mob_stats/minecraft/zombie.json` :
```json
{
  "entity_type": "minecraft:zombie",
  "category": "melee",
  "base_levels": {
    "BRUTE_FORCE": 15, "PHYSICAL_ENDURANCE": 12, "AGILITY": 5,
    "PHYSICAL_RESISTANCE": 8, "TRACKING": 10
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/skeleton.json` :
```json
{
  "entity_type": "minecraft:skeleton",
  "category": "ranged",
  "base_levels": {
    "PRECISION": 20, "AGILITY": 10, "TRACKING": 15, "PHYSICAL_ENDURANCE": 5
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/creeper.json` :
```json
{
  "entity_type": "minecraft:creeper",
  "category": "melee",
  "base_levels": {
    "AGILITY": 8, "TRACKING": 12, "WILLPOWER": 20
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/spider.json` :
```json
{
  "entity_type": "minecraft:spider",
  "category": "melee",
  "base_levels": {
    "AGILITY": 18, "RAPIDITE": 12, "TRACKING": 14, "KEEN_SENSES": 15
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/enderman.json` :
```json
{
  "entity_type": "minecraft:enderman",
  "category": "melee",
  "base_levels": {
    "ARCANE_POWER": 25, "AGILITY": 20, "PHYSICAL_ENDURANCE": 15,
    "TRACKING": 30, "WILLPOWER": 20
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/witch.json` :
```json
{
  "entity_type": "minecraft:witch",
  "category": "magic",
  "base_levels": {
    "ARCANE_POWER": 30, "ALCHEMY": 35, "MAGIC_RESISTANCE": 20,
    "CASTING_SPEED": 15, "ERUDITION": 10
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/blaze.json` :
```json
{
  "entity_type": "minecraft:blaze",
  "category": "magic",
  "base_levels": {
    "FIRE_AFFINITY": 50, "ARCANE_POWER": 20, "MAGIC_RESISTANCE": 15,
    "PHYSICAL_RESISTANCE": 10
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/wither_skeleton.json` :
```json
{
  "entity_type": "minecraft:wither_skeleton",
  "category": "melee",
  "base_levels": {
    "BRUTE_FORCE": 25, "PHYSICAL_ENDURANCE": 20, "PHYSICAL_RESISTANCE": 15,
    "WILLPOWER": 18, "INTIMIDATION": 12
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/piglin.json` :
```json
{
  "entity_type": "minecraft:piglin",
  "category": "melee",
  "base_levels": {
    "BRUTE_FORCE": 12, "AGILITY": 10, "PHYSICAL_RESISTANCE": 8, "TRACKING": 12
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/pillager.json` :
```json
{
  "entity_type": "minecraft:pillager",
  "category": "ranged",
  "base_levels": {
    "PRECISION": 25, "AGILITY": 12, "PHYSICAL_ENDURANCE": 8, "TRACKING": 18
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/evoker.json` :
```json
{
  "entity_type": "minecraft:evoker",
  "category": "magic",
  "base_levels": {
    "ARCANE_POWER": 35, "CASTING_SPEED": 20, "MAGIC_RESISTANCE": 25,
    "ERUDITION": 20, "INTIMIDATION": 15
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/wither.json` :
```json
{
  "entity_type": "minecraft:wither",
  "category": "boss",
  "base_levels": {
    "BRUTE_FORCE": 60, "PHYSICAL_ENDURANCE": 70, "ARCANE_POWER": 50,
    "MAGIC_RESISTANCE": 40, "PHYSICAL_RESISTANCE": 45, "WILLPOWER": 60,
    "INTIMIDATION": 50, "AIR_AFFINITY": 30
  }
}
```

Créer `src/main/resources/data/statmod/mob_stats/minecraft/elder_guardian.json` :
```json
{
  "entity_type": "minecraft:elder_guardian",
  "category": "boss",
  "base_levels": {
    "PHYSICAL_ENDURANCE": 55, "MAGIC_RESISTANCE": 45, "TRACKING": 50,
    "PHYSICAL_RESISTANCE": 40, "WILLPOWER": 35, "WATER_AFFINITY": 60
  }
}
```

- [ ] **Step 5.2 : Créer `MobStatReloadListener.java`**

```java
package tong.statmod.reload;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import tong.statmod.STATMod;
import tong.statmod.stats.StatType;

import java.util.*;

public class MobStatReloadListener extends SimpleJsonResourceReloadListener {

    public static final MobStatReloadListener INSTANCE = new MobStatReloadListener();

    /** entityType → niveaux par stat (index → niveau). */
    private final Map<ResourceLocation, int[]> mobDefaults = new HashMap<>();

    private MobStatReloadListener() {
        super(new GsonBuilder().create(), "mob_stats");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        mobDefaults.clear();
        for (var entry : objects.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                ResourceLocation entityType =
                    new ResourceLocation(obj.get("entity_type").getAsString());
                int[] levels = new int[StatType.values().length];
                JsonObject baseLevels = obj.getAsJsonObject("base_levels");
                for (StatType stat : StatType.values()) {
                    if (baseLevels.has(stat.name())) {
                        levels[stat.index] = baseLevels.get(stat.name()).getAsInt();
                    }
                }
                mobDefaults.put(entityType, levels);
            } catch (Exception e) {
                STATMod.LOGGER.error("[MobStats] Failed to load {}: {}", entry.getKey(), e.getMessage());
            }
        }
        STATMod.LOGGER.info("[MobStats] Loaded {} mob stat entries", mobDefaults.size());
    }

    public Optional<int[]> getDefaults(ResourceLocation entityType) {
        return Optional.ofNullable(mobDefaults.get(entityType));
    }
}
```

- [ ] **Step 5.3 : Créer `MobStatInitializer.java`**

```java
package tong.statmod.stats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.reload.MobStatReloadListener;

public class MobStatInitializer {

    /** Initialise les stats d'un mob depuis les JSON sans L2H. */
    public static void applyDefaults(Mob mob) {
        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityType == null) return;

        mob.getCapability(MobStatsProvider.MOB_STATS).ifPresent(stats -> {
            MobStatReloadListener.INSTANCE.getDefaults(entityType).ifPresent(defaults -> {
                stats.reset();
                for (int i = 0; i < defaults.length && i < MobStats.STAT_COUNT; i++) {
                    stats.setLevel(i, defaults[i]);
                }
            });
        });
    }
}
```

- [ ] **Step 5.4 : Enregistrer `MobStatReloadListener` dans `STATMod.java`**

Dans `STATMod.java`, dans la méthode qui écoute `AddReloadListenerEvent` (ou créer si absent) :

```java
@SubscribeEvent
public static void onAddReloadListeners(AddReloadListenerEvent event) {
    event.addListener(BossRewardReloadListener.INSTANCE);  // déjà ajouté en Phase 0
    event.addListener(MobStatReloadListener.INSTANCE);      // ← AJOUT
}
```

- [ ] **Step 5.5 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 5.6 : Commit**

```bash
git add src/main/java/tong/statmod/reload/MobStatReloadListener.java \
        src/main/java/tong/statmod/stats/MobStatInitializer.java \
        src/main/resources/data/statmod/mob_stats/ \
        src/main/java/tong/statmod/STATMod.java
git commit -m "feat: add MobStatReloadListener + JSON defaults for 13 vanilla mobs"
```

---

## Task 6 : CapabilityHandler — attacher MobStats aux mobs

**Fichiers :**
- Modifier : `src/main/java/tong/statmod/capability/CapabilityHandler.java`

- [ ] **Step 6.1 : Ajouter le register + attach dans `CapabilityHandler.java`**

```java
// Dans registerCapabilities() — ajouter :
event.register(MobStats.class);

// Dans attachCapabilities() — ajouter APRÈS le bloc Player :
if (event.getObject() instanceof net.minecraft.world.entity.Mob) {
    event.addCapability(
        ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "mob_stats"),
        new MobStatsProvider());
}
```

- [ ] **Step 6.2 : Créer l'event handler d'initialisation des stats mob**

Dans `CapabilityHandler.java`, ajouter :

```java
@SubscribeEvent
public static void onMobJoin(EntityJoinLevelEvent event) {
    if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
    if (mob.level().isClientSide()) return;
    // Init stats une seule fois (flag NBT pour éviter double-init)
    if (mob.getPersistentData().getBoolean("statmod_stats_initialized")) return;
    mob.getPersistentData().putBoolean("statmod_stats_initialized", true);

    // L2H présent → sync via L2H (fait dans L2HostilityMobSync)
    // L2H absent → niveaux par défaut depuis JSON
    if (!net.minecraftforge.fml.ModList.get().isLoaded("l2hostility")) {
        tong.statmod.stats.MobStatInitializer.applyDefaults(mob);
        tong.statmod.stats.MobStatEffectApplier.applyAllBonuses(mob);
    }
}
```

- [ ] **Step 6.3 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 6.4 : Commit**

```bash
git add src/main/java/tong/statmod/capability/CapabilityHandler.java
git commit -m "feat: attach MobStats capability to all Mob entities on join"
```

---

## Task 7 : L2HostilityMobSync — bridge L2H → MobStats

**Fichiers :**
- Créer : `src/main/java/tong/statmod/integration/L2HostilityMobSync.java`
- Modifier : `src/main/java/tong/statmod/STATMod.java` (init si L2H présent)
- Modifier : `src/main/resources/META-INF/mods.toml`

- [ ] **Step 7.1 : Ajouter dépendance optionnelle dans `mods.toml`**

Dans `src/main/resources/META-INF/mods.toml`, ajouter dans la section `[[dependencies.statmod]]` :

```toml
[[dependencies.statmod]]
    modId="l2hostility"
    mandatory=false
    versionRange="[2.5.0,)"
    ordering="NONE"
    side="BOTH"
```

- [ ] **Step 7.2 : Créer `L2HostilityMobSync.java`**

```java
package tong.statmod.integration;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.stats.MobStatEffectApplier;
import tong.statmod.stats.StatType;

/**
 * Seulement instancié si l2hostility est chargé (vérifié dans STATMod.commonSetup()).
 * Lit MobTraitCap.lv + traits actifs → calcule MobStats.
 */
@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class L2HostilityMobSync {

    // scaleFactor : points de stat par niveau L2H (configurable plus tard)
    private static final float SCALE_FACTOR = 2.0f;

    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        if (mob.getPersistentData().getBoolean("statmod_stats_initialized")) return;
        mob.getPersistentData().putBoolean("statmod_stats_initialized", true);

        MobTraitCap.HOLDER.get(mob).ifPresent(traitCap -> {
            mob.getCapability(MobStatsProvider.MOB_STATS).ifPresent(stats -> {
                int l2hLevel = traitCap.lv;
                applyBaseScaling(stats, l2hLevel);
                applyTraitBonuses(stats, traitCap);
            });
            MobStatEffectApplier.applyAllBonuses(mob);
        });
    }

    private static void applyBaseScaling(
        tong.statmod.capability.MobStats stats, int l2hLevel) {
        // Stats de combat : poids 1.0
        for (StatType stat : new StatType[]{
            StatType.BRUTE_FORCE, StatType.BLADE_TECHNIQUE, StatType.RAPIDITE,
            StatType.AGILITY, StatType.PHYSICAL_RESISTANCE,
            StatType.PHYSICAL_ENDURANCE, StatType.PRECISION
        }) {
            stats.setLevel(stat.index, Math.round(l2hLevel * SCALE_FACTOR));
        }
        // Stats magiques : poids 0.7
        for (StatType stat : new StatType[]{
            StatType.ARCANE_POWER, StatType.WATER_AFFINITY, StatType.EARTH_AFFINITY,
            StatType.FIRE_AFFINITY, StatType.AIR_AFFINITY,
            StatType.MAGIC_RESISTANCE, StatType.CASTING_SPEED,
            StatType.MANA_POOL, StatType.ERUDITION
        }) {
            stats.setLevel(stat.index, Math.round(l2hLevel * SCALE_FACTOR * 0.7f));
        }
        // Stats survie/mental : poids 0.5
        for (StatType stat : new StatType[]{
            StatType.TRACKING, StatType.KEEN_SENSES,
            StatType.INTIMIDATION, StatType.WILLPOWER
        }) {
            stats.setLevel(stat.index, Math.round(l2hLevel * SCALE_FACTOR * 0.5f));
        }
    }

    private static void applyTraitBonuses(
        tong.statmod.capability.MobStats stats, MobTraitCap traitCap) {
        for (var entry : traitCap.traits.entrySet()) {
            applyTraitBonus(stats, entry.getKey());
        }
    }

    private static void applyTraitBonus(
        tong.statmod.capability.MobStats stats, MobTrait trait) {
        ResourceLocation id = trait.getRegistryName();
        if (id == null) return;
        switch (id.getPath()) {
            case "fiery"        -> stats.addToLevel(StatType.FIRE_AFFINITY.index,      30);
            case "regen"        -> stats.addToLevel(StatType.PHYSICAL_ENDURANCE.index, 25);
            case "reflect"      -> stats.addToLevel(StatType.PHYSICAL_RESISTANCE.index,20);
            case "invisible"    -> { stats.addToLevel(StatType.AGILITY.index, 20);
                                     stats.addToLevel(StatType.KEEN_SENSES.index, 15); }
            case "gravity"      -> stats.addToLevel(StatType.BRUTE_FORCE.index,        15);
            case "aura"         -> stats.addToLevel(StatType.INTIMIDATION.index,       25);
            case "shulker"      -> stats.addToLevel(StatType.AGILITY.index,            20);
            case "drain"        -> { stats.addToLevel(StatType.ARCANE_POWER.index, 30);
                                     stats.addToLevel(StatType.MANA_POOL.index,    20); }
            case "adapting"     -> stats.addToLevel(StatType.PHYSICAL_RESISTANCE.index,10);
            case "arena"        -> stats.addToLevel(StatType.INTIMIDATION.index,       30);
            case "corrosion"    -> stats.addToLevel(StatType.ALCHEMY.index,            10);
            case "growth"       -> { stats.addToLevel(StatType.PHYSICAL_ENDURANCE.index,30);
                                     stats.addToLevel(StatType.BRUTE_FORCE.index,      20); }
            case "reprint"      -> stats.addToLevel(StatType.WILLPOWER.index,          25);
            case "split"        -> { stats.addToLevel(StatType.WILLPOWER.index, 20);
                                     stats.addToLevel(StatType.AGILITY.index,   15); }
            case "killer_aura"  -> { stats.addToLevel(StatType.ARCANE_POWER.index,  40);
                                     stats.addToLevel(StatType.INTIMIDATION.index, 30); }
            case "undying"      -> { stats.addToLevel(StatType.WILLPOWER.index,            50);
                                     stats.addToLevel(StatType.PHYSICAL_ENDURANCE.index,   30); }
            case "dementor"     -> { stats.addToLevel(StatType.MAGIC_RESISTANCE.index, 40);
                                     stats.addToLevel(StatType.WILLPOWER.index,        35); }
            case "dispell"      -> { stats.addToLevel(StatType.ARCANE_POWER.index, 35);
                                     stats.addToLevel(StatType.ERUDITION.index,    25); }
            case "master"       -> { stats.addToLevel(StatType.INTIMIDATION.index, 40);
                                     stats.addToLevel(StatType.WILLPOWER.index,    30); }
            case "ragnarok"     -> { stats.addToLevel(StatType.BRUTE_FORCE.index,  40);
                                     stats.addToLevel(StatType.ARCANE_POWER.index, 30); }
            default -> { /* trait inconnu : pas de bonus */ }
        }
    }
}
```

- [ ] **Step 7.3 : Enregistrer dans `STATMod.java`**

Dans `commonSetup()` de `STATMod.java`, ajouter :

```java
if (ModList.get().isLoaded("l2hostility")) {
    MinecraftForge.EVENT_BUS.register(L2HostilityMobSync.class);
    LOGGER.info("[STAT Mod] L2Hostility detected — mob stat sync enabled");
}
```

> Important : ne pas mettre `@Mod.EventBusSubscriber` sur `L2HostilityMobSync` — il est enregistré manuellement seulement si L2H est présent. Retirer l'annotation si elle est présente dans le code ci-dessus.

- [ ] **Step 7.4 : Ajouter la dépendance compileOnly dans `build.gradle`**

Dans `build.gradle`, dans la section `dependencies {}` :

```groovy
// L2Hostility — optional compat (coordinates from CurseForge at implementation time)
// compileOnly fg.deobf("curse.maven:l2hostility-<curseid>:<fileid>")
// À décommenter avec les vraies coordonnées maven lors du build
```

Pour compiler maintenant sans L2H, utiliser le JAR local cloné :

```groovy
compileOnly files("${rootDir}/../L2Hostility/build/libs/l2hostility-2.5.19.jar")
```

OU compiler conditionnellement avec `@Optional.Interface` si le JAR n'est pas dispo. Pour l'instant, si le JAR n'est pas compilé, la classe `L2HostilityMobSync` ne sera pas incluse dans le build principal.

**Alternative sans JAR L2H disponible** : créer une interface `IL2HCompat` et charger `L2HostilityMobSync` via réflexion. Cette approche est documentée dans `ARCHITECTURE.md` pour implémentation ultérieure.

- [ ] **Step 7.5 : Vérifier build (sans L2H)**

```
./gradlew build -x validateStatModPackaging
```

- [ ] **Step 7.6 : Commit**

```bash
git add src/main/java/tong/statmod/integration/L2HostilityMobSync.java \
        src/main/java/tong/statmod/STATMod.java \
        src/main/resources/META-INF/mods.toml
git commit -m "feat: add L2HostilityMobSync — L2H traits/level → MobStats (20 trait mappings)"
```

---

## Task 8 : XP joueur scalé par niveau L2H + résistances

**Fichiers :**
- Modifier : `src/main/java/tong/statmod/progression/CombatXPHandler.java`
- Créer : `src/main/java/tong/statmod/combat/L2HPlayerResistance.java`

- [ ] **Step 8.1 : Modifier `CombatXPHandler.java` pour multiplier l'XP par niveau L2H**

Dans `onLivingHurt`, après la vérification que le joueur est l'attaquant, ajouter le calcul du multiplicateur L2H. La méthode `awardXp` de `ActionXpHelper` est déjà appelée — ajouter un override du multiplicateur avant l'appel ou modifier `ActionXpHelper.awardXp` pour accepter un multiplicateur externe.

La modification la moins invasive : passer par `awardXpWithL2HScale` — nouvelle méthode statique dans `CombatXPHandler` :

```java
// Ajouter dans CombatXPHandler.java :

private static float getL2HXpMultiplier(net.minecraft.world.entity.LivingEntity target) {
    if (!net.minecraftforge.fml.ModList.get().isLoaded("l2hostility")) return 1.0f;
    return dev.xkmc.l2hostility.content.capability.mob.MobTraitCap.HOLDER
        .get(target)
        .map(cap -> 1.0f + cap.lv * 0.05f)
        .orElse(1.0f);
}
```

Dans `onLivingHurt`, remplacer chaque appel direct à `ActionXpHelper.awardXp(player, stat, tier)` par :

```java
int xpBoost = tier.minXp() + player.getRandom().nextInt(tier.maxXp() - tier.minXp() + 1);
if (RandomEvents.isBonusXpActive()) xpBoost *= 2;
float l2hMult = getL2HXpMultiplier(event.getEntity());
// Déléguer avec l'XP déjà calculé :
final int finalXp = Math.round(xpBoost * l2hMult);
```

> Note : cela nécessite d'exposer une surcharge `ActionXpHelper.awardXpFixed(player, statIndex, xpAmount)` ou de modifier directement le flow. Voir la section ci-dessous.

- [ ] **Step 8.2 : Ajouter `awardXpFixed` dans `ActionXpHelper.java`**

Ajouter dans `ActionXpHelper.java` la surcharge suivante (après la méthode existante) :

```java
/** Accorde un montant XP fixe (déjà calculé, avec multiplicateurs externes appliqués). */
public static void awardXpFixed(ServerPlayer player, int statIndex, int xp) {
    CapabilityHelper.withStats(player, stats -> {
        int oldLevel = stats.getLevel(statIndex);
        stats.addXp(statIndex, xp);
        int newLevel = stats.getLevel(statIndex);
        if (newLevel > oldLevel && newLevel > 0) {
            LevelUpHandler.onLevelUp(player, statIndex, newLevel);
        }
        NetworkHandler.sendToPlayer(
            new StatUpdatePacket(statIndex, newLevel, stats.getXp(statIndex)), player);
        DailyChallenge.onXpAward(player, StatType.byIndex(statIndex), xp);

        if (PartyManager.isInParty(player.getUUID())) {
            java.util.List<ServerPlayer> allPlayers = player.getServer().getPlayerList().getPlayers();
            java.util.Collection<ServerPlayer> allies = FtbTeamsIntegration.getAllies(player, allPlayers);
            float share = PartyManager.getPartyShareBonus(PartyManager.getPartyMembers(player.getUUID()).size());
            for (ServerPlayer ally : allies) {
                if (ally.distanceTo(player) < 50) {
                    CapabilityHelper.withStats(ally, ms -> ms.addXp(statIndex, Math.round(xp * share)));
                }
            }
        }
    });
}
```

- [ ] **Step 8.3 : Créer `L2HPlayerResistance.java`**

```java
package tong.statmod.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;

/**
 * Réduit les dégâts des traits L2H selon les stats du joueur.
 * Enregistré manuellement seulement si l2hostility est présent.
 */
public class L2HPlayerResistance {

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var src = event.getSource();
        var attacker = src.getEntity();
        if (!(attacker instanceof net.minecraft.world.entity.Mob mob)) return;

        dev.xkmc.l2hostility.content.capability.mob.MobTraitCap.HOLDER
            .get(mob).ifPresent(traitCap -> {
                CapabilityHelper.withStats(player, stats -> {
                    float reduction = computeReduction(stats, traitCap, src);
                    if (reduction > 0) {
                        event.setAmount(event.getAmount() * (1.0f - reduction));
                    }
                });
            });
    }

    private static float computeReduction(
        tong.statmod.capability.PlayerStats stats,
        dev.xkmc.l2hostility.content.capability.mob.MobTraitCap traitCap,
        net.minecraft.world.damagesource.DamageSource src) {

        float totalReduction = 0f;

        for (var trait : traitCap.traits.keySet()) {
            var id = trait.getRegistryName();
            if (id == null) continue;
            totalReduction += switch (id.getPath()) {
                case "fiery"       -> stats.getLevel(StatType.FIRE_AFFINITY.index)     / 200.0f;
                case "drain"       -> stats.getLevel(StatType.ARCANE_POWER.index)      / 200.0f;
                case "killer_aura" -> stats.getLevel(StatType.MAGIC_RESISTANCE.index)  / 200.0f;
                case "corrosion"   -> stats.getLevel(StatType.ALCHEMY.index)           / 200.0f;
                case "dementor"    -> stats.getLevel(StatType.WILLPOWER.index)         / 100.0f;
                case "gravity"     -> stats.getLevel(StatType.PHYSICAL_RESISTANCE.index)/ 200.0f;
                case "arena"       -> stats.getLevel(StatType.WILLPOWER.index)         / 150.0f;
                default            -> 0f;
            };
        }

        // Cap total à 75% de réduction
        return Math.min(totalReduction, 0.75f);
    }
}
```

- [ ] **Step 8.4 : Enregistrer `L2HPlayerResistance` dans `STATMod.java`**

Dans le même bloc `if (ModList.get().isLoaded("l2hostility"))` de `commonSetup()` :

```java
MinecraftForge.EVENT_BUS.register(L2HPlayerResistance.class);
```

- [ ] **Step 8.5 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 8.6 : Commit**

```bash
git add src/main/java/tong/statmod/progression/CombatXPHandler.java \
        src/main/java/tong/statmod/progression/ActionXpHelper.java \
        src/main/java/tong/statmod/combat/L2HPlayerResistance.java \
        src/main/java/tong/statmod/STATMod.java
git commit -m "feat: L2H XP scaling (+5%/level) + player resistance vs L2H traits"
```

---

## Task 9 : Mettre à jour ARCHITECTURE.md

**Fichier :**
- Modifier : `src/main/java/tong/statmod/ARCHITECTURE.md` (ou `docs/ARCHITECTURE.md`)

- [ ] **Step 9.1 : Mettre à jour le graphe de dépendances**

Ajouter dans le graphe :

```
STATMod
├── ... (existant)
├── MobStats capability (NOUVEAU)
│   ├── MobStatsProvider
│   ├── MobStatCalculator
│   ├── MobStatEffectApplier
│   ├── MobStatInitializer  ← data/statmod/mob_stats/*.json
│   └── MobStatReloadListener
└── integration/ (optionnel)
    ├── L2HostilityMobSync  ← si l2hostility chargé
    └── L2HPlayerResistance ← si l2hostility chargé
```

- [ ] **Step 9.2 : Commit**

```bash
git add ARCHITECTURE.md
git commit -m "docs: update ARCHITECTURE.md with MobStats + L2H integration"
```

---

## Résumé Phase 1

| Tâche | Fonctionnalité | Fichiers créés/modifiés |
|-------|----------------|------------------------|
| T1+T2 | MobStats capability + Provider | 3 |
| T3 | MobStatCalculator | 2 |
| T4 | MobStatEffectApplier | 1 |
| T5 | JSON vanilla mobs + ReloadListener + Initializer | 16 |
| T6 | Attach capability aux mobs | 1 |
| T7 | L2HostilityMobSync (20 trait mappings) | 3 |
| T8 | XP scaling L2H + résistances joueur | 4 |
| T9 | Docs | 1 |

**Critères de succès :**
- Sans L2H : zombie avec `BRUTE_FORCE = 15`, follow range augmentée
- Avec L2H : zombie niveau 20 → `BRUTE_FORCE = 40`, mob Fiery → `FIRE_AFFINITY += 30`
- Tuer mob L2H niveau 20 → ×2 XP STAT
- Joueur `FIRE_AFFINITY = 80` → -40% dégâts d'un mob Fiery
- Build sans L2H installé : `BUILD SUCCESSFUL`

**Prochaine étape → Phase 2 : Mob Skills Engine** (plan sera rédigé après validation de Phase 1)
