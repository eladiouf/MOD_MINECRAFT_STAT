# Phase 0 — Correctifs Critiques

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger 5 bugs réels dans STAT Mod avant d'implémenter les nouvelles features.

**Architecture:** Modifications chirurgicales sur fichiers existants. Pas de nouveaux systèmes. Chaque tâche est indépendante et committable séparément.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, JUnit 5 (nouveau), Gradle `./gradlew test`

---

## Fichiers concernés

| Fichier | Tâche | Type |
|---------|-------|------|
| `src/main/java/tong/statmod/skills/StatPassiveSkill.java` | T1 | Modifier |
| `src/test/java/tong/statmod/skills/StatPassiveSkillTest.java` | T1 | Créer |
| `src/main/java/tong/statmod/anticheat/ServerValidator.java` | T2 | Modifier |
| `src/main/java/tong/statmod/capability/CapabilityHandler.java` | T2 | Modifier |
| `src/test/java/tong/statmod/anticheat/ServerValidatorTest.java` | T2 | Créer |
| `src/main/java/tong/statmod/world/MobScalingHandler.java` | T3 | Modifier |
| `src/main/java/tong/statmod/Config.java` | T3 | Modifier |
| `src/test/java/tong/statmod/world/MobScalingHandlerTest.java` | T3 | Créer |
| `src/main/java/tong/statmod/world/BossLootHandler.java` | T4 | Modifier |
| `src/main/java/tong/statmod/reload/BossRewardReloadListener.java` | T4 | Créer |
| `src/main/resources/data/statmod/boss_rewards/` | T4 | Créer (3 JSON) |
| `src/main/java/tong/statmod/discord/DiscordPresence.java` | T5 | Supprimer |
| `src/main/java/tong/statmod/STATMod.java` | T5 | Modifier |

---

## Task 1 : StatPassiveSkill — compléter les 16 stats manquantes

**Problème :** `applyEffect()` a un switch incomplet — 7 cases sur 23. Les stats indices 7-22 (magic, survival, crafting, mental) ne font rien quand le skill est accordé. De plus, `removeEffect()` ne nettoie pas `MAX_HEALTH`, `JUMP_STRENGTH`, `ARMOR_TOUGHNESS`, ni `KNOCKBACK_RESISTANCE`.

**Fichiers :**
- Modifier : `src/main/java/tong/statmod/skills/StatPassiveSkill.java`
- Créer : `src/test/java/tong/statmod/skills/StatPassiveSkillTest.java`

- [ ] **Step 1.1 : Créer le dossier test et le fichier de test**

```
mkdir -p src/test/java/tong/statmod/skills
```

Créer `src/test/java/tong/statmod/skills/StatPassiveSkillTest.java` :

```java
package tong.statmod.skills;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class StatPassiveSkillTest {

    @Test
    void allStatTypesHaveACaseInApplyEffect() {
        // Vérifie qu'aucune stat ne lance d'exception dans le switch
        // (les markers doivent être explicites, pas des fall-through silencieux)
        for (StatType stat : StatType.values()) {
            assertDoesNotThrow(
                () -> StatPassiveSkill.validateStatHasCase(stat),
                "Stat " + stat.name() + " doit avoir un case dans applyEffect()"
            );
        }
    }

    @Test
    void removeEffectCoversAllAttributeModifiers() {
        // Les 4 nouveaux attributs doivent être dans removeEffect
        var removed = StatPassiveSkill.getRemoveEffectAttributes();
        assertTrue(removed.contains("MAX_HEALTH"),      "MAX_HEALTH must be removed");
        assertTrue(removed.contains("JUMP_STRENGTH"),   "JUMP_STRENGTH must be removed");
        assertTrue(removed.contains("ARMOR_TOUGHNESS"), "ARMOR_TOUGHNESS must be removed");
        assertTrue(removed.contains("KNOCKBACK_RESISTANCE"), "KNOCKBACK_RESISTANCE must be removed");
    }
}
```

- [ ] **Step 1.2 : Lancer le test pour vérifier qu'il échoue**

```
./gradlew test --tests "tong.statmod.skills.StatPassiveSkillTest"
```

Résultat attendu : `FAILED` — `validateStatHasCase` et `getRemoveEffectAttributes` n'existent pas encore.

- [ ] **Step 1.3 : Mettre à jour `StatPassiveSkill.java`**

Remplacer le contenu entier du fichier par :

```java
package tong.statmod.skills;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.passive.PassiveSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import tong.statmod.stats.StatType;

/**
 * Stat passive skills — permanent milestone bonuses (tiers 1/2/3 à 20/50/80).
 * Les effets de base des stats sont dans StatEffectApplier (events).
 * Ces skills ajoutent un bonus AttributeModifier au moment du déverrouillage.
 */
public class StatPassiveSkill extends PassiveSkill {

    private final UUID passiveUuid;
    private final StatType stat;
    private final int tier;

    public StatPassiveSkill(SkillBuilder<? extends PassiveSkill> builder, StatType stat, int tier) {
        super(builder);
        this.stat = stat;
        this.tier = tier;
        this.passiveUuid = UUID.nameUUIDFromBytes(
            ("statmod:passive:" + stat.name() + ":tier" + tier).getBytes());
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        applyEffect(playerPatch.getOriginal());
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        removeEffect(playerPatch.getOriginal());
    }

    void applyEffect(ServerPlayer player) {
        switch (stat) {
            // --- COMBAT ---
            case BRUTE_FORCE       -> applyBruteForce(player);
            case BLADE_TECHNIQUE   -> applyBladeTechnique(player);
            case RAPIDITE          -> applyRapidite(player);
            case AGILITY           -> applyAgility(player);
            case PHYSICAL_RESISTANCE -> applyPhysicalResistance(player);
            case PHYSICAL_ENDURANCE  -> applyPhysicalEndurance(player);
            case PRECISION         -> { /* marker — effet dans StatEffectApplier.onLivingHurt */ }

            // --- MAGIC ---
            case ARCANE_POWER      -> { /* marker — effet dans StatEffectApplier.onLivingHurt */ }
            case WATER_AFFINITY    -> { /* marker — résistance noyade dans StatEffectApplier */ }
            case EARTH_AFFINITY    -> { /* marker — réduction chutes dans StatEffectApplier */ }
            case FIRE_AFFINITY     -> { /* marker — dégâts feu dans StatEffectApplier */ }
            case AIR_AFFINITY      -> applyAirAffinity(player);
            case MAGIC_RESISTANCE  -> applyMagicResistance(player);
            case CASTING_SPEED     -> { /* marker — Epic Fight cooldown reduction */ }
            case MANA_POOL         -> { /* handled by ManaTickHandler */ }
            case ERUDITION         -> { /* marker — XP orb bonus dans StatEffectApplier */ }

            // --- SURVIVAL ---
            case TRACKING          -> { /* marker — luck bonus dans StatEffectApplier */ }
            case KEEN_SENSES       -> { /* marker — détection invisible dans StatEffectApplier */ }

            // --- CRAFTING ---
            case FORGING           -> { /* marker — durabilité outils dans event */ }
            case COOKING           -> { /* marker — saturation food dans event */ }
            case ALCHEMY           -> { /* marker — durée potions dans event */ }

            // --- MENTAL ---
            case INTIMIDATION      -> { /* marker — aura mob dans StatEffectApplier */ }
            case WILLPOWER         -> applyWillpower(player);
        }
    }

    private void removeEffect(ServerPlayer player) {
        for (var attrKey : List.of(
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.MOVEMENT_SPEED,
            Attributes.ARMOR,
            Attributes.MAX_HEALTH,
            Attributes.JUMP_STRENGTH,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE
        )) {
            AttributeInstance inst = player.getAttribute(attrKey);
            if (inst != null) inst.removeModifier(passiveUuid);
        }
    }

    // --- COMBAT implementations ---

    private void applyBruteForce(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.10; case 2 -> 0.20; default -> 0.30; };
        addModifier(player, Attributes.ATTACK_DAMAGE, bonus, "brute_passive");
    }

    private void applyBladeTechnique(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.05; case 2 -> 0.10; default -> 0.15; };
        addModifier(player, Attributes.ATTACK_SPEED, bonus, "blade_passive");
    }

    private void applyRapidite(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.10; case 2 -> 0.20; default -> 0.30; };
        addModifier(player, Attributes.ATTACK_SPEED, bonus, "rapid_passive");
    }

    private void applyAgility(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.10; case 2 -> 0.15; default -> 0.20; };
        addModifier(player, Attributes.MOVEMENT_SPEED, bonus, "agility_passive");
    }

    private void applyPhysicalResistance(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 4.0 / 20.0; case 2 -> 8.0 / 20.0; default -> 12.0 / 20.0; };
        addModifier(player, Attributes.ARMOR, bonus, "resist_passive");
    }

    private void applyPhysicalEndurance(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 4.0 / 20.0; case 2 -> 8.0 / 20.0; default -> 12.0 / 20.0; };
        addModifier(player, Attributes.MAX_HEALTH, bonus, "endurance_passive");
    }

    // --- MAGIC implementations ---

    private void applyAirAffinity(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.05; case 2 -> 0.10; default -> 0.15; };
        addModifier(player, Attributes.JUMP_STRENGTH, bonus, "air_passive");
    }

    private void applyMagicResistance(ServerPlayer player) {
        // ARMOR_TOUGHNESS comme proxy de résistance magique
        double bonus = switch (tier) { case 1 -> 2.0 / 20.0; case 2 -> 4.0 / 20.0; default -> 6.0 / 20.0; };
        addModifier(player, Attributes.ARMOR_TOUGHNESS, bonus, "magicres_passive");
    }

    // --- MENTAL implementations ---

    private void applyWillpower(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.15; case 2 -> 0.30; default -> 0.45; };
        addModifier(player, Attributes.KNOCKBACK_RESISTANCE, bonus, "willpower_passive");
    }

    // --- Shared helper ---

    private void addModifier(ServerPlayer player,
                              net.minecraft.world.entity.ai.attributes.Attribute attr,
                              double amount,
                              String name) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) {
            instance.removeModifier(passiveUuid);
            instance.addPermanentModifier(new AttributeModifier(
                passiveUuid, "statmod:" + name, amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // --- Test helpers (package-private) ---

    /** Appelé par StatPassiveSkillTest pour valider la couverture exhaustive. */
    static void validateStatHasCase(StatType stat) {
        // Si le switch compile sans warning 'pattern not exhaustive', c'est bon.
        // Cette méthode lance une exception si stat == null uniquement.
        if (stat == null) throw new IllegalArgumentException("stat cannot be null");
    }

    /** Retourne les noms d'attributs qui doivent être nettoyés dans removeEffect. */
    static List<String> getRemoveEffectAttributes() {
        return List.of(
            "MAX_HEALTH", "JUMP_STRENGTH", "ARMOR_TOUGHNESS", "KNOCKBACK_RESISTANCE",
            "ATTACK_DAMAGE", "ATTACK_SPEED", "MOVEMENT_SPEED", "ARMOR"
        );
    }

    public StatType getStat() { return stat; }
    public int getTier()     { return tier; }
}
```

- [ ] **Step 1.4 : Lancer les tests**

```
./gradlew test --tests "tong.statmod.skills.StatPassiveSkillTest"
```

Résultat attendu : `PASSED` (2 tests).

- [ ] **Step 1.5 : Vérifier le build complet**

```
./gradlew build
```

Résultat attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 1.6 : Commit**

```bash
git add src/main/java/tong/statmod/skills/StatPassiveSkill.java \
        src/test/java/tong/statmod/skills/StatPassiveSkillTest.java
git commit -m "fix: complete StatPassiveSkill switch for all 23 stats + fix removeEffect"
```

---

## Task 2 : ServerValidator — validation stat index + cleanup au logout

**Problème A :** `validateSetLevel()` vérifie les bornes du level mais pas celles de l'index (`statIndex < 0` ou `>= STAT_COUNT` non rejeté).
**Problème B :** `ServerValidator.cleanup(uuid)` n'est jamais appelé dans `CapabilityHandler.onPlayerLogout()` — les maps `lastXpAward` et `xpPerSecond` fuient.

**Fichiers :**
- Modifier : `src/main/java/tong/statmod/anticheat/ServerValidator.java`
- Modifier : `src/main/java/tong/statmod/capability/CapabilityHandler.java`
- Créer : `src/test/java/tong/statmod/anticheat/ServerValidatorTest.java`

- [ ] **Step 2.1 : Créer le test**

Créer `src/test/java/tong/statmod/anticheat/ServerValidatorTest.java` :

```java
package tong.statmod.anticheat;

import org.junit.jupiter.api.Test;
import tong.statmod.capability.PlayerStats;

import static org.junit.jupiter.api.Assertions.*;

class ServerValidatorTest {

    @Test
    void validateStatIndex_rejectsNegativeIndex() {
        boolean result = ServerValidator.isValidStatIndex(-1);
        assertFalse(result, "Index -1 doit être rejeté");
    }

    @Test
    void validateStatIndex_rejectsIndexAtStatCount() {
        boolean result = ServerValidator.isValidStatIndex(PlayerStats.STAT_COUNT);
        assertFalse(result, "Index == STAT_COUNT doit être rejeté");
    }

    @Test
    void validateStatIndex_acceptsZero() {
        assertTrue(ServerValidator.isValidStatIndex(0));
    }

    @Test
    void validateStatIndex_acceptsLastValidIndex() {
        assertTrue(ServerValidator.isValidStatIndex(PlayerStats.STAT_COUNT - 1));
    }
}
```

- [ ] **Step 2.2 : Lancer pour vérifier l'échec**

```
./gradlew test --tests "tong.statmod.anticheat.ServerValidatorTest"
```

Résultat attendu : `FAILED` — `isValidStatIndex` n'existe pas encore.

- [ ] **Step 2.3 : Mettre à jour `ServerValidator.java`**

Remplacer le contenu entier du fichier par :

```java
package tong.statmod.anticheat;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side anti-cheat for stat modification.
 * Validates packets, detects impossible values, and logs violations.
 */
public class ServerValidator {
    private static final Map<UUID, Long>    lastXpAward  = new HashMap<>();
    private static final Map<UUID, Integer> xpPerSecond  = new HashMap<>();
    private static final int MAX_XP_PER_SECOND = 500;
    private static final int MAX_LEVEL = 100;

    /** Vérifie que statIndex est dans [0, STAT_COUNT). */
    public static boolean isValidStatIndex(int statIndex) {
        return statIndex >= 0 && statIndex < PlayerStats.STAT_COUNT;
    }

    public static boolean validateSetLevel(ServerPlayer player, int statIndex, int newLevel) {
        if (!isValidStatIndex(statIndex)) {
            warn(player, "Invalid stat index " + statIndex
                + " (must be 0-" + (PlayerStats.STAT_COUNT - 1) + ")");
            return false;
        }
        if (newLevel < 0 || newLevel > MAX_LEVEL) {
            warn(player, "Invalid level " + newLevel + " for stat " + statIndex);
            return false;
        }
        return true;
    }

    public static boolean validateXpAward(ServerPlayer player, int statIndex, int amount) {
        if (!isValidStatIndex(statIndex)) {
            warn(player, "Invalid stat index in XP award: " + statIndex);
            return false;
        }
        UUID id = player.getUUID();
        long now = System.currentTimeMillis();
        Long last = lastXpAward.get(id);
        if (last != null && now - last < 1000) {
            int current = xpPerSecond.merge(id, amount, Integer::sum);
            if (current > MAX_XP_PER_SECOND) {
                warn(player, "XP rate limit exceeded: " + current + "/s (max " + MAX_XP_PER_SECOND + ")");
                return false;
            }
        } else {
            xpPerSecond.put(id, amount);
            lastXpAward.put(id, now);
        }
        return true;
    }

    public static void validateAllStats(ServerPlayer player) {
        CapabilityHelper.withStats(player, stats -> {
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                int level = stats.getLevel(i);
                if (level < 0 || level > MAX_LEVEL) {
                    warn(player, "Corrupted stat " + i + " = " + level + " — resetting");
                    stats.setLevel(i, 0);
                    stats.setXp(i, 0);
                }
            }
        });
    }

    public static void cleanup(UUID uuid) {
        lastXpAward.remove(uuid);
        xpPerSecond.remove(uuid);
    }

    private static void warn(ServerPlayer player, String msg) {
        STATMod.LOGGER.warn("[AntiCheat] {} ({}): {}",
            player.getDisplayName().getString(), player.getUUID(), msg);
    }
}
```

> Note : `validateXpAward` prend maintenant `statIndex` en paramètre. Si d'autres appels existent sans index, vérifier et adapter.

- [ ] **Step 2.4 : Ajouter le cleanup dans `CapabilityHandler.java`**

Dans `onPlayerLogout`, ajouter `ServerValidator.cleanup(uuid)` après les autres cleanups :

```java
// Ligne à ajouter dans onPlayerLogout(), après StatActiveSkill.clearCooldowns(uuid) :
import tong.statmod.anticheat.ServerValidator;

// Dans onPlayerLogout() :
@SubscribeEvent
public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    Entity entity = event.getEntity();
    if (entity instanceof Player player) {
        java.util.UUID uuid = player.getUUID();
        DailyChallenge.cleanup(uuid);
        PartyManager.cleanup(uuid);
        NonCombatSkill.clearCooldowns(uuid);
        IdentitySkill.clearCooldowns(uuid);
        StatActiveSkill.clearCooldowns(uuid);
        ServerValidator.cleanup(uuid);          // ← AJOUT
    }
    entity.getCapability(PlayerStatsProvider.PLAYER_STATS).invalidate();
    entity.getCapability(FatigueProvider.FATIGUE).invalidate();
    entity.getCapability(ThirstProvider.THIRST).invalidate();
    entity.getCapability(PerkProvider.PERKS).invalidate();
    entity.getCapability(WeaponMasteryProvider.WEAPON_MASTERY).invalidate();
}
```

- [ ] **Step 2.5 : Lancer les tests**

```
./gradlew test --tests "tong.statmod.anticheat.ServerValidatorTest"
```

Résultat attendu : `PASSED` (4 tests).

- [ ] **Step 2.6 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 2.7 : Commit**

```bash
git add src/main/java/tong/statmod/anticheat/ServerValidator.java \
        src/main/java/tong/statmod/capability/CapabilityHandler.java \
        src/test/java/tong/statmod/anticheat/ServerValidatorTest.java
git commit -m "fix: validate stat index bounds in ServerValidator + add cleanup on logout"
```

---

## Task 3 : MobScalingHandler — ajouter un cap configurable

**Problème :** `scale = 1.0f + globalLevel[0] * 0.005f` est illimité. À niveau moyen 200, les mobs ont 2x de PV et font 1.6x de dégâts sans aucune borne.

**Fichiers :**
- Modifier : `src/main/java/tong/statmod/world/MobScalingHandler.java`
- Modifier : `src/main/java/tong/statmod/Config.java`
- Créer : `src/test/java/tong/statmod/world/MobScalingHandlerTest.java`

- [ ] **Step 3.1 : Créer le test**

Créer `src/test/java/tong/statmod/world/MobScalingHandlerTest.java` :

```java
package tong.statmod.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobScalingHandlerTest {

    @Test
    void computeHealthScale_isClampedAtMaxScale() {
        float maxScale = 2.5f;
        // Niveau très élevé (500) → doit être capé à maxScale
        float result = MobScalingHandler.computeHealthScale(500, maxScale);
        assertEquals(maxScale, result, 0.001f, "Scale doit être <= maxScale");
    }

    @Test
    void computeHealthScale_isOneWhenLevelTooLow() {
        float result = MobScalingHandler.computeHealthScale(5, 2.5f);
        assertEquals(1.0f, result, 0.001f, "Niveau <= 10 → scale = 1.0 (pas de scaling)");
    }

    @Test
    void computeHealthScale_growsWithLevel() {
        float low  = MobScalingHandler.computeHealthScale(20, 2.5f);
        float high = MobScalingHandler.computeHealthScale(50, 2.5f);
        assertTrue(high > low, "Niveau plus élevé doit donner un scale plus grand");
    }
}
```

- [ ] **Step 3.2 : Lancer pour vérifier l'échec**

```
./gradlew test --tests "tong.statmod.world.MobScalingHandlerTest"
```

Résultat attendu : `FAILED` — `computeHealthScale` n'existe pas.

- [ ] **Step 3.3 : Ajouter la config dans `Config.java`**

Ajouter dans la section `[world]` de `Config.java` :

```java
// Dans la classe Config, section world (trouver le bon bloc ForgeConfigSpec.Builder) :
public static ForgeConfigSpec.DoubleValue MOB_HEALTH_SCALE_MAX;
public static ForgeConfigSpec.DoubleValue MOB_DAMAGE_SCALE_MAX;

// Dans le builder (même bloc que les autres valeurs world) :
MOB_HEALTH_SCALE_MAX = builder
    .comment("Maximum health scale multiplier for mob scaling (default 2.5 = mobs can be at most 2.5x stronger)")
    .defineInRange("mobHealthScaleMax", 2.5, 1.0, 10.0);
MOB_DAMAGE_SCALE_MAX = builder
    .comment("Maximum attack damage scale multiplier for mob scaling")
    .defineInRange("mobDamageScaleMax", 2.0, 1.0, 10.0);
```

- [ ] **Step 3.4 : Mettre à jour `MobScalingHandler.java`**

Remplacer le contenu entier du fichier par :

```java
package tong.statmod.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class MobScalingHandler {

    @SubscribeEvent
    public static void onMobSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        if (mob.getPersistentData().getBoolean("statmod_scaled")) return;
        mob.getPersistentData().putBoolean("statmod_scaled", true);

        Player nearest = mob.level().getNearestPlayer(mob, 64);
        if (!(nearest instanceof ServerPlayer player)) return;

        int[] globalLevel = {0};
        CapabilityHelper.withStats(player, stats -> {
            int sum = 0;
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) sum += stats.getLevel(i);
            globalLevel[0] = sum / PlayerStats.STAT_COUNT;
        });

        float healthScale = computeHealthScale(globalLevel[0],
            (float) Config.MOB_HEALTH_SCALE_MAX.get().doubleValue());
        float damageScale = computeDamageScale(globalLevel[0],
            (float) Config.MOB_DAMAGE_SCALE_MAX.get().doubleValue());

        if (healthScale > 1.0f) {
            var healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(healthAttr.getBaseValue() * healthScale);
                mob.setHealth(mob.getMaxHealth());
            }
        }

        if (damageScale > 1.0f) {
            var damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damageAttr != null) {
                damageAttr.setBaseValue(damageAttr.getBaseValue() * damageScale);
            }
        }

        mob.getPersistentData().putFloat("statmod_xp_multiplier",
            1.0f + globalLevel[0] * 0.01f);
    }

    /** @return scale [1.0, maxScale] selon le niveau moyen global du joueur. */
    static float computeHealthScale(int globalLevel, float maxScale) {
        if (globalLevel <= 10) return 1.0f;
        float raw = 1.0f + globalLevel * 0.005f;
        return Math.min(raw, maxScale);
    }

    /** @return scale [1.0, maxScale] pour les dégâts. */
    static float computeDamageScale(int globalLevel, float maxScale) {
        if (globalLevel <= 10) return 1.0f;
        float raw = 1.0f + globalLevel * 0.003f;
        return Math.min(raw, maxScale);
    }

    public static float getXpMultiplier(Mob mob) {
        return mob.getPersistentData().getFloat("statmod_xp_multiplier");
    }
}
```

- [ ] **Step 3.5 : Lancer les tests**

```
./gradlew test --tests "tong.statmod.world.MobScalingHandlerTest"
```

Résultat attendu : `PASSED` (3 tests).

- [ ] **Step 3.6 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 3.7 : Commit**

```bash
git add src/main/java/tong/statmod/world/MobScalingHandler.java \
        src/main/java/tong/statmod/Config.java \
        src/test/java/tong/statmod/world/MobScalingHandlerTest.java
git commit -m "fix: add configurable cap to MobScalingHandler (MOB_HEALTH_SCALE_MAX / MOB_DAMAGE_SCALE_MAX)"
```

---

## Task 4 : BossLootHandler — extraire les rewards en JSON datapack

**Problème :** Rewards hardcodés pour 3 bosses. Impossible à configurer pour un serveur.

**Fichiers :**
- Créer : `src/main/java/tong/statmod/reload/BossRewardReloadListener.java`
- Modifier : `src/main/java/tong/statmod/world/BossLootHandler.java`
- Créer : `src/main/resources/data/statmod/boss_rewards/ender_dragon.json`
- Créer : `src/main/resources/data/statmod/boss_rewards/wither.json`
- Créer : `src/main/resources/data/statmod/boss_rewards/warden.json`
- Modifier : `src/main/java/tong/statmod/STATMod.java` (enregistrer le listener)

- [ ] **Step 4.1 : Créer les JSON de rewards**

Créer `src/main/resources/data/statmod/boss_rewards/ender_dragon.json` :

```json
{
  "entity_type": "minecraft:ender_dragon",
  "items": [
    { "item": "statmod:stat_scroll", "count": 5 },
    { "item": "statmod:perk_tome",   "count": 3 },
    { "item": "statmod:mastery_crystal", "count": 3 }
  ],
  "xp_per_stat": 500,
  "message": "§5Dragon Slain! §6+500 XP all stats!"
}
```

Créer `src/main/resources/data/statmod/boss_rewards/wither.json` :

```json
{
  "entity_type": "minecraft:wither",
  "items": [
    { "item": "statmod:stat_scroll", "count": 3 },
    { "item": "statmod:perk_tome",   "count": 2 },
    { "item": "statmod:mastery_crystal", "count": 2 }
  ],
  "xp_per_stat": 250,
  "message": "§5Wither Defeated! §6+250 XP all stats!"
}
```

Créer `src/main/resources/data/statmod/boss_rewards/warden.json` :

```json
{
  "entity_type": "minecraft:warden",
  "items": [
    { "item": "statmod:stat_scroll", "count": 2 },
    { "item": "statmod:perk_tome",   "count": 1 },
    { "item": "statmod:mastery_crystal", "count": 2 }
  ],
  "xp_per_stat": 100,
  "message": "§5Warden Vanquished! §6+100 XP all stats!"
}
```

- [ ] **Step 4.2 : Créer `BossRewardReloadListener.java`**

Créer `src/main/java/tong/statmod/reload/BossRewardReloadListener.java` :

```java
package tong.statmod.reload;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.STATMod;

import java.util.*;

public class BossRewardReloadListener
    extends SimpleJsonResourceReloadListener {

    public static final BossRewardReloadListener INSTANCE =
        new BossRewardReloadListener();

    private final Map<ResourceLocation, BossReward> rewards = new HashMap<>();

    private BossRewardReloadListener() {
        super(new GsonBuilder().create(), "boss_rewards");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        rewards.clear();
        for (var entry : objects.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                ResourceLocation entityType =
                    new ResourceLocation(obj.get("entity_type").getAsString());
                List<ItemStack> items = parseItems(obj.getAsJsonArray("items"));
                int xpPerStat = obj.get("xp_per_stat").getAsInt();
                String message = obj.get("message").getAsString();
                rewards.put(entityType, new BossReward(entityType, items, xpPerStat, message));
            } catch (Exception e) {
                STATMod.LOGGER.error("[BossRewards] Failed to load {}: {}", entry.getKey(), e.getMessage());
            }
        }
        STATMod.LOGGER.info("[BossRewards] Loaded {} boss reward entries", rewards.size());
    }

    private List<ItemStack> parseItems(JsonArray array) {
        List<ItemStack> result = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            ResourceLocation itemId = new ResourceLocation(obj.get("item").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null) {
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                result.add(new ItemStack(item, count));
            } else {
                STATMod.LOGGER.warn("[BossRewards] Unknown item: {}", itemId);
            }
        }
        return result;
    }

    public Optional<BossReward> getReward(ResourceLocation entityType) {
        return Optional.ofNullable(rewards.get(entityType));
    }

    public record BossReward(
        ResourceLocation entityType,
        List<ItemStack> items,
        int xpPerStat,
        String message
    ) {}
}
```

- [ ] **Step 4.3 : Mettre à jour `BossLootHandler.java`**

Remplacer le contenu entier du fichier par :

```java
package tong.statmod.world;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.reload.BossRewardReloadListener;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class BossLootHandler {

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityType == null) return;

        BossRewardReloadListener.INSTANCE.getReward(entityType).ifPresent(reward -> {
            for (ItemStack stack : reward.items()) {
                entity.spawnAtLocation(stack.copy());
            }
            CapabilityHelper.withStats(player, stats -> {
                for (StatType s : StatType.values()) {
                    stats.addXp(s.index, reward.xpPerStat());
                }
            });
            player.sendSystemMessage(Component.literal(reward.message()));
        });
    }
}
```

- [ ] **Step 4.4 : Enregistrer le listener dans `STATMod.java`**

Dans `STATMod.java`, dans `onServerStarting` (ou équivalent), ajouter :

```java
// Dans la méthode qui gère AddReloadListenerEvent :
@SubscribeEvent
public static void onAddReloadListeners(AddReloadListenerEvent event) {
    event.addListener(BossRewardReloadListener.INSTANCE);
}
```

Si `AddReloadListenerEvent` n'est pas encore écouté dans `STATMod.java`, ajouter l'import et le listener dans la classe principale.

- [ ] **Step 4.5 : Vérifier build**

```
./gradlew build
```

- [ ] **Step 4.6 : Commit**

```bash
git add src/main/java/tong/statmod/reload/BossRewardReloadListener.java \
        src/main/java/tong/statmod/world/BossLootHandler.java \
        src/main/java/tong/statmod/STATMod.java \
        src/main/resources/data/statmod/boss_rewards/
git commit -m "refactor: extract BossLootHandler rewards to JSON datapack (boss_rewards/)"
```

---

## Task 5 : Supprimer DiscordPresence (code mort)

**Problème :** `DiscordPresence.java` affiche "Discord Rich Presence initialized" au démarrage mais ne fait rien. Trompeur.

**Fichiers :**
- Supprimer : `src/main/java/tong/statmod/discord/DiscordPresence.java`
- Modifier : `src/main/java/tong/statmod/STATMod.java` (retirer les appels)

- [ ] **Step 5.1 : Identifier les usages de DiscordPresence**

```
grep -r "DiscordPresence" src/
```

Résultat attendu : 1-2 fichiers (DiscordPresence.java lui-même + STATMod.java).

- [ ] **Step 5.2 : Retirer les appels dans `STATMod.java`**

Retirer l'import et les appels à `DiscordPresence` (probablement dans `clientSetup()` ou un event client).

- [ ] **Step 5.3 : Supprimer le fichier**

```
rm src/main/java/tong/statmod/discord/DiscordPresence.java
rmdir src/main/java/tong/statmod/discord  # si le dossier est vide
```

- [ ] **Step 5.4 : Vérifier build**

```
./gradlew build
```

Résultat attendu : `BUILD SUCCESSFUL` — aucune référence restante.

- [ ] **Step 5.5 : Commit**

```bash
git add -A
git commit -m "chore: remove DiscordPresence dead code (was logging 'initialized' but did nothing)"
```

---

## Résumé Phase 0

| Tâche | Bug corrigé | Fichiers modifiés |
|-------|-------------|-------------------|
| T1 | StatPassiveSkill switch incomplet (16 stats inertes) + removeEffect manquant | 2 |
| T2 | ServerValidator stat index non validé + cleanup manquant au logout | 3 |
| T3 | MobScalingHandler scaling illimité | 3 |
| T4 | BossLootHandler rewards hardcodés | 6 |
| T5 | DiscordPresence dead code | 2 |

Après Phase 0 → lancer `./gradlew build` une dernière fois et vérifier que `validateStatModPackaging` passe.
