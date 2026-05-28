# STAT Mod – Level-Up Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich the level-up system with tiered XP actions, passive stat bonuses, milestone rewards, and a perk system. Rebalance thirst and fatigue.

**Architecture:** Modify existing handler classes (`CombatXPHandler`, `NonCombatXPHandler`, `StatEffectApplier`, `FatigueHandler`) and add new classes (`ActionXpHelper`, `PerkManager`, `PerkProvider`, `PerkScreen`). All data persisted via Forge capabilities.

**Tech Stack:** Minecraft Forge 1.20.1, Epic Fight 20.14.17, Java 17

---

### Task 1: Thirst & Fatigue rebalance

**Files:**
- Modify: `src/main/java/tong/statmod/fatigue/FatigueHandler.java`
- Modify: `src/main/java/tong/statmod/world/thirst/ThirstHandler.java`
- Modify: `src/main/java/tong/statmod/Config.java`

- [ ] **Step 1: Reduce water bottle thirst restore to 8**

In `FatigueHandler.java`, change the water restore amount:
```java
// Line ~143: change 20 to 8
thirst.addThirst(8);
```

- [ ] **Step 2: Add thirst cost to fatigue actions**

In `FatigueHandler.java`, add thirst cost alongside each fatigue cost. Inject `ThirstProvider` and reduce thirst:

```java
// After each fatigue cost, add thirst cost
// Add this helper method:
private static void addThirstCost(ServerPlayer player, float amount) {
    player.getCapability(ThirstProvider.THIRST).ifPresent(thirst -> {
        thirst.reduceThirst(amount);
        NetworkHandler.sendToPlayer(new ThirstPacket(thirst.getThirst()), player);
    });
}

// In onLivingTick, after sprint fatigue cost:
addThirstCost(player, 0.03f);

// After jump fatigue cost:
addThirstCost(player, 0.05f);

// In onLivingAttack, after damage fatigue cost:
addThirstCost(player, 1.0f);

// In onBlockBreak, after block break fatigue cost:
addThirstCost(player, 0.3f);
```

- [ ] **Step 3: Eating food reduces fatigue**

In `FatigueHandler.java`, add a new subscriber method:

```java
@SubscribeEvent
public static void onPlayerFinishUse(net.minecraftforge.event.entity.player.PlayerItemUseFinishEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    ItemStack result = event.getResultStack();
    if (result.getItem().isEdible()) {
        FoodProperties food = result.getItem().getFoodProperties();
        if (food != null) {
            float fatigueReduction = 5.0f + food.getNutrition() * 2.0f;
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.reduceFatigue(fatigueReduction);
                syncIfChanged(player, fatigue);
            });
        }
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew build`

---

### Task 2: ActionXpHelper – tier-based XP utility

**Files:**
- Create: `src/main/java/tong/statmod/progression/ActionXpHelper.java`

- [ ] **Step 1: Create ActionXpHelper class**

```java
package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.PlayerStats;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

public class ActionXpHelper {

    public enum XpTier {
        COMMON(2, 3),
        INTERMEDIATE(5, 8),
        RARE(12, 20);

        public final int minXp;
        public final int maxXp;

        XpTier(int min, int max) {
            this.minXp = min;
            this.maxXp = max;
        }
    }

    public static void awardXp(ServerPlayer player, int statIndex, XpTier tier) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int xp = tier.minXp + player.getRandom().nextInt(tier.maxXp - tier.minXp + 1);
            stats.addXp(statIndex, xp);
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(statIndex, stats.getLevel(statIndex), stats.getXp(statIndex)),
                player);
        });
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew build`

---

### Task 3: Rewrite CombatXPHandler with tier actions

**Files:**
- Modify: `src/main/java/tong/statmod/progression/CombatXPHandler.java`

- [ ] **Step 1: Replace entire CombatXPHandler content**

```java
package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.stats.StatType;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class CombatXPHandler {
    private static final Map<UUID, HitTracker> hitTrackers = new HashMap<>();
    private static final Map<UUID, ComboTracker> comboTrackers = new HashMap<>();
    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // ★ Common: weapon hit
        awardWeaponXp(player);

        // ★★ Intermediate: combo (3 hits in 2s)
        trackCombo(player);

        // ★★ Intermediate: overkill
        if (event.getEntity().getHealth() <= 0 && event.getAmount() > 20) {
            ActionXpHelper.awardXp(player, StatType.BRUTE_FORCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★ Intermediate: hit without being touched for 5s
        long now = System.currentTimeMillis();
        Long lastHit = lastDamageTime.get(player.getUUID());
        if (lastHit != null && now - lastHit > 5000) {
            ActionXpHelper.awardXp(player, StatType.BLADE_TECHNIQUE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★★ Rare: hit 3 different mobs with 1 swing (detected by rapid switch)
        trackRapidHit(player);
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        lastDamageTime.put(player.getUUID(), System.currentTimeMillis());

        // ★ Common: take damage
        ActionXpHelper.awardXp(player, StatType.PHYSICAL_RESISTANCE.index, ActionXpHelper.XpTier.COMMON);

        // ★★ Intermediate: survive with <4 hearts
        if (player.getHealth() / player.getMaxHealth() < 0.2f) {
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_RESISTANCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★ Intermediate: block
        if (player.isBlocking()) {
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_ENDURANCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★ Intermediate: take damage from 3 sources in 10s (use tracker)
        trackDamageSources(player);

        // ★★ Common: low hp willpower
        if (player.getHealth() / player.getMaxHealth() < 0.3f) {
            ActionXpHelper.awardXp(player, StatType.WILLPOWER.index, ActionXpHelper.XpTier.COMMON);
        }

        // ★★★ Rare: survive with 1/2 heart
        if (player.getHealth() - event.getAmount() <= 1.0f && player.getHealth() > 0) {
            ActionXpHelper.awardXp(player, StatType.WILLPOWER.index, ActionXpHelper.XpTier.RARE);
        }
    }

    private static void awardWeaponXp(ServerPlayer player) {
        StatType stat = determinePrimaryStat(player);
        if (stat == null) return;
        ActionXpHelper.awardXp(player, stat.index, ActionXpHelper.XpTier.COMMON);
    }

    private static StatType determinePrimaryStat(ServerPlayer player) {
        var cap = player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
        if (cap.isPresent() && cap.resolve().isPresent()) {
            Object patch = cap.resolve().get();
            if (patch instanceof ServerPlayerPatch playerPatch) {
                CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                if (itemCap != null && !itemCap.isEmpty()) {
                    var cat = itemCap.getWeaponCategory();
                    if (cat == CapabilityItem.WeaponCategories.AXE || cat == CapabilityItem.WeaponCategories.GREATSWORD)
                        return StatType.BRUTE_FORCE;
                    if (cat == CapabilityItem.WeaponCategories.SWORD || cat == CapabilityItem.WeaponCategories.DAGGER
                        || cat == CapabilityItem.WeaponCategories.UCHIGATANA || cat == CapabilityItem.WeaponCategories.TACHI
                        || cat == CapabilityItem.WeaponCategories.TRIDENT || cat == CapabilityItem.WeaponCategories.LONGSWORD)
                        return StatType.BLADE_TECHNIQUE;
                    if (cat == CapabilityItem.WeaponCategories.BOW || cat == CapabilityItem.WeaponCategories.CROSSBOW)
                        return StatType.PRECISION;
                    if (cat == CapabilityItem.WeaponCategories.SPEAR)
                        return StatType.AGILITY;
                    if (cat == CapabilityItem.WeaponCategories.SHIELD)
                        return StatType.PHYSICAL_ENDURANCE;
                    if (cat == CapabilityItem.WeaponCategories.FIST)
                        return StatType.RAPIDITE;
                }
            }
        }
        return null;
    }

    private static void trackRapidHit(ServerPlayer player) {
        long now = System.currentTimeMillis();
        HitTracker tracker = hitTrackers.computeIfAbsent(player.getUUID(), k -> new HitTracker());
        tracker.addHit(now);
        if (tracker.getHitCount(2000) >= 5) {
            ActionXpHelper.awardXp(player, StatType.RAPIDITE.index, ActionXpHelper.XpTier.INTERMEDIATE);
            tracker.reset();
        }
    }

    private static void trackCombo(ServerPlayer player) {
        long now = System.currentTimeMillis();
        ComboTracker combo = comboTrackers.computeIfAbsent(player.getUUID(), k -> new ComboTracker());
        combo.addHit(now);
        if (combo.getHitCount(2000) >= 3) {
            ActionXpHelper.awardXp(player, StatType.BLADE_TECHNIQUE.index, ActionXpHelper.XpTier.INTERMEDIATE);
            combo.reset();
        }
    }

    // Track unique damage sources in 10s window
    private static final Map<UUID, DamageSourceTracker> dmgSourceTrackers = new HashMap<>();
    private static void trackDamageSources(ServerPlayer player) {
        // Simplified: just award XP for taking damage from non-physical sources
        DamageSourceTracker tracker = dmgSourceTrackers.computeIfAbsent(player.getUUID(), k -> new DamageSourceTracker());
        tracker.recordSource();
        if (tracker.getUniqueSourceCount() >= 3) {
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_RESISTANCE.index, ActionXpHelper.XpTier.RARE);
            tracker.reset();
        }
    }

    // Hit tracker (same as before)
    private static class HitTracker {
        private final long[] hits = new long[20];
        private int index = 0;
        void addHit(long time) { hits[index % hits.length] = time; index++; }
        int getHitCount(long windowMs) {
            long threshold = System.currentTimeMillis() - windowMs;
            int count = 0;
            for (long h : hits) { if (h >= threshold) count++; }
            return count;
        }
        void reset() { for (int i = 0; i < hits.length; i++) hits[i] = 0; }
    }

    private static class ComboTracker {
        private final long[] hits = new long[10];
        private int index = 0;
        void addHit(long time) { hits[index % hits.length] = time; index++; }
        int getHitCount(long windowMs) {
            long threshold = System.currentTimeMillis() - windowMs;
            int count = 0;
            for (long h : hits) { if (h >= threshold) count++; }
            return count;
        }
        void reset() { for (int i = 0; i < hits.length; i++) hits[i] = 0; }
    }

    private static class DamageSourceTracker {
        private final java.util.HashSet<String> sources = new java.util.HashSet<>();
        private long lastReset = System.currentTimeMillis();
        void recordSource() {
            if (System.currentTimeMillis() - lastReset > 10000) reset();
            sources.add("damage");
        }
        int getUniqueSourceCount() { return sources.size(); }
        void reset() { sources.clear(); lastReset = System.currentTimeMillis(); }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew build`

---

### Task 4: Rewrite NonCombatXPHandler with tier actions

**Files:**
- Modify: `src/main/java/tong/statmod/progression/NonCombatXPHandler.java`

- [ ] **Step 1: Update all XP awards to use ActionXpHelper tiers**

Replace the `awardXp` method usage with `ActionXpHelper.awardXp` using appropriate tiers. Key changes:

```java
// Mob kills
@SubscribeEvent
public static void onMobKill(LivingDeathEvent event) {
    if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
    LivingEntity killed = event.getEntity();

    if (killed instanceof EnderDragon || killed instanceof WitherBoss || killed.getMaxHealth() > 200) {
        ActionXpHelper.awardXp(player, StatType.INTIMIDATION.index, ActionXpHelper.XpTier.RARE);
        ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.RARE);
    } else if (killed instanceof Monster) {
        ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.COMMON);
    }
}

// Exploration
@SubscribeEvent
public static void onExplore(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
        ActionXpHelper.awardXp(player, StatType.KEEN_SENSES.index, ActionXpHelper.XpTier.INTERMEDIATE);
    }
}

// Crafting
@SubscribeEvent
public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
        ActionXpHelper.awardXp(player, StatType.FORGING.index, ActionXpHelper.XpTier.COMMON);
        if (event.getCrafting().isEnchanted()) {
            ActionXpHelper.awardXp(player, StatType.ERUDITION.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }
    }
}

// Smelting
@SubscribeEvent
public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
        ActionXpHelper.awardXp(player, StatType.COOKING.index, ActionXpHelper.XpTier.COMMON);
    }
}

// Mining – deep blocks give more XP
@SubscribeEvent
public static void onMine(BlockEvent.BreakEvent event) {
    if (!(event.getPlayer() instanceof ServerPlayer player)) return;
    if (event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
        if (event.getPos().getY() < 0) {
            ActionXpHelper.awardXp(player, StatType.EARTH_AFFINITY.index, ActionXpHelper.XpTier.INTERMEDIATE);
        } else {
            ActionXpHelper.awardXp(player, StatType.EARTH_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
        }
    }
}

// Magic damage (standby, keep basic)
@SubscribeEvent
public static void onDealMagicDamage(LivingDamageEvent event) {
    if (event.getSource().getEntity() instanceof ServerPlayer player) {
        // Magic stat on standby, skip
    }
}

// Potion hit (standby, keep basic)
@SubscribeEvent
public static void onGetPotionHit(LivingDamageEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
        ActionXpHelper.awardXp(player, StatType.WILLPOWER.index, ActionXpHelper.XpTier.COMMON);
    }
}

// Item use → Casting Speed (standby, skip)
@SubscribeEvent
public static void onUsePotion(PlayerInteractEvent.RightClickItem event) {
    // Casting Speed on standby
}

// Water/fire/air ticks
@SubscribeEvent
public static void onTick(PlayerTickEvent event) {
    if (!(event.player instanceof ServerPlayer player) || event.phase != TickEvent.Phase.END) return;
    if (player.tickCount % 100 != 0) return;

    if (player.isInWater() || player.isUnderWater()) {
        ActionXpHelper.awardXp(player, StatType.WATER_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
    }
    if (player.isInLava() || player.isOnFire()) {
        ActionXpHelper.awardXp(player, StatType.FIRE_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
    }
    if (!player.onGround() && player.getDeltaMovement().y > 0.5) {
        ActionXpHelper.awardXp(player, StatType.AIR_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew build`

---

### Task 5: Expand StatEffectApplier with all passive bonuses

**Files:**
- Modify: `src/main/java/tong/statmod/stats/StatEffectApplier.java`

- [ ] **Step 1: Add all passive stat bonuses on login and level-up**

```java
package tong.statmod.stats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {
    private static final UUID ENDURANCE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Attacker bonuses
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                float multiplier = 1.0f;
                multiplier += StatCalculator.getDamageBonus(stats.getLevel(StatType.BRUTE_FORCE.index));
                multiplier += StatCalculator.getDamageBonus(stats.getLevel(StatType.BLADE_TECHNIQUE.index));
                event.setAmount(event.getAmount() * multiplier);
            });
        }

        // Defender bonuses
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                float reduction = StatCalculator.getDamageReduction(stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
                event.setAmount(event.getAmount() * (1.0f - reduction));
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAllBonuses(player);
        }
    }

    public static void applyAllBonuses(ServerPlayer player) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            // Endurance → bonus hearts
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.removeModifier(ENDURANCE_UUID);
                float bonusHearts = StatCalculator.getEnduranceHearts(stats.getLevel(StatType.PHYSICAL_ENDURANCE.index));
                if (bonusHearts > 0) {
                    maxHealth.addPermanentModifier(new AttributeModifier(
                        ENDURANCE_UUID, "Endurance Bonus", bonusHearts, AttributeModifier.Operation.ADDITION));
                }
            }

            // Agility → move speed
            AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (moveSpeed != null) {
                moveSpeed.removeModifier(SPEED_UUID);
                float speedBonus = StatCalculator.getMoveSpeedBonus(stats.getLevel(StatType.AGILITY.index));
                if (speedBonus > 0) {
                    moveSpeed.addPermanentModifier(new AttributeModifier(
                        SPEED_UUID, "Agility Bonus", speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Rapidité → attack speed
            AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.removeModifier(ATTACK_SPEED_UUID);
                float atkSpeedBonus = StatCalculator.getAttackSpeedBonus(stats.getLevel(StatType.RAPIDITE.index));
                if (atkSpeedBonus > 0) {
                    attackSpeed.addPermanentModifier(new AttributeModifier(
                        ATTACK_SPEED_UUID, "Rapidité Bonus", atkSpeedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        });
    }
}
```

- [ ] **Step 2: Expand StatCalculator with missing passive bonus formulas**

In `StatCalculator.java`, add:
```java
// Crit chance
public static float getCritChance(int precisionLevel) {
    return 0.003f * precisionLevel; // 0.3% per level, max 30%
}

// XP bonus (Erudition)
public static float getXpBonus(int eruditionLevel) {
    return 0.005f * eruditionLevel; // 0.5% per level, max 50%
}

// Swim speed (Water Affinity)
public static float getSwimSpeedBonus(int waterLevel) {
    return 0.005f * waterLevel; // 0.5% per level, max 50%
}

// Mining speed (Earth Affinity)
public static float getMiningSpeedBonus(int earthLevel) {
    return 0.003f * earthLevel; // 0.3% per level, max 30%
}

// Fire damage bonus (Fire Affinity)
public static float getFireDamageBonus(int fireLevel) {
    return 0.005f * fireLevel; // 0.5% per level, max 50%
}

// Jump height (Air Affinity)
public static float getJumpBonus(int airLevel) {
    return 0.003f * airLevel; // 0.3% per level, max 30%
}

// Magic damage reduction (Magic Resistance)
public static float getMagicReduction(int magicResLevel) {
    return 0.003f * magicResLevel; // 0.3% per level, max 30%
}

// Item use speed (Casting Speed)
public static float getItemUseSpeed(int castingLevel) {
    return 0.003f * castingLevel; // 0.3% per level, max 30%
}

// Tool durability (Forging)
public static float getDurabilityBonus(int forgingLevel) {
    return 0.003f * forgingLevel; // 0.3% per level, max 30%
}

// Saturation bonus (Cooking)
public static float getSaturationBonus(int cookingLevel) {
    return 0.003f * cookingLevel; // 0.3% per level, max 30%
}

// Potion duration (Alchemy)
public static float getPotionDurationBonus(int alchemyLevel) {
    return 0.003f * alchemyLevel; // 0.3% per level, max 30%
}

// Detection range (Tracking)
public static float getTrackingRange(int trackingLevel) {
    return 0.003f * trackingLevel; // 0.3% per level, max 30%
}

// Entity detection (Keen Senses)
public static float getDetectionRange(int keenLevel) {
    return 0.003f * keenLevel; // 0.3% per level, max 30%
}

// Fear range (Intimidation)
public static float getFearRange(int intimidateLevel) {
    return 0.003f * intimidateLevel; // 0.3% per level, max 30%
}

// Status duration reduction (Willpower)
public static float getStatusDurationReduction(int willpowerLevel) {
    return 0.005f * willpowerLevel; // 0.5% per level, max 50%
}

// Magic damage bonus (Arcane Power)
public static float getMagicDamageBonus(int arcaneLevel) {
    return 0.003f * arcaneLevel; // 0.3% per level, max 30%
}

// Mana bonus
public static int getManaBonus(int manaLevel) {
    return manaLevel; // +1 per level, max +100
}
```

- [ ] **Step 3: Build**

Run: `./gradlew build`

---

### Task 6: Milestone system (every 10 levels)

**Files:**
- Create: `src/main/java/tong/statmod/progression/LevelUpHandler.java`
- Modify: `src/main/java/tong/statmod/capability/PlayerStats.java`
- Modify: `src/main/java/tong/statmod/client/notification/LevelUpToast.java`

- [ ] **Step 1: Add level-up callback in PlayerStats**

In `PlayerStats.java`, add a consumer/callback field:

```java
import java.util.function.BiConsumer;

public class PlayerStats implements INBTSerializable<CompoundTag> {
    // ...
    private transient BiConsumer<Integer, Integer> onLevelUp; // (statIndex, newLevel)

    public void setOnLevelUp(BiConsumer<Integer, Integer> callback) {
        this.onLevelUp = callback;
    }

    @Override
    public void addXp(int index, int amount) {
        if (index < 0 || index >= STAT_COUNT) return;
        this.xp[index] += amount;
        while (levels[index] < 100) {
            int required = getXpForNextLevel(levels[index]);
            if (this.xp[index] < required) break;
            this.xp[index] -= required;
            levels[index]++;
            STATMod.LOGGER.debug("Level up! Stat {} → level {}", index, levels[index]);
            if (onLevelUp != null) {
                onLevelUp.accept(index, levels[index]);
            }
        }
    }
}
```

- [ ] **Step 2: Create LevelUpHandler for milestone rewards**

```java
package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.integration.EpicFightCompat;
import tong.statmod.skills.SkillUnlockRegistry;
import tong.statmod.stats.StatType;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class LevelUpHandler {

    public static void onLevelUp(ServerPlayer player, int statIndex, int newLevel) {
        // Check milestones every 10 levels
        if (newLevel % 10 == 0) {
            int tier = newLevel / 10;
            handleMilestone(player, statIndex, tier, newLevel);
        }
    }

    private static void handleMilestone(ServerPlayer player, int statIndex, int tier, int level) {
        // Play sound
        player.level().playSound(null, player.blockPosition(),
            level == 100 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
            SoundSource.PLAYERS, 1.0f, 1.0f);

        // Grant perk points at 50 and 100
        int perkPoints = 0;
        if (level == 50) perkPoints = 1;
        if (level == 100) perkPoints = 3;

        // Grant Epic Fight skills at tier thresholds
        StatType stat = StatType.byIndex(statIndex);
        int skillTier = switch (level) {
            case 10 -> 0;
            case 30 -> 1;
            case 50 -> 2;
            case 70 -> 3;
            default -> -1;
        };
        if (skillTier >= 0) {
            var skill = SkillUnlockRegistry.getSkill(stat, skillTier);
            if (skill != null) {
                EpicFightCompat.grantSkill(player, skill);
            }
        }

        // Sync stats to client
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(statIndex, stats.getLevel(statIndex), stats.getXp(statIndex)),
                player);
        });

        // Apply passive bonus refresh
        tong.statmod.stats.StatEffectApplier.applyAllBonuses(player);
    }
}
```

- [ ] **Step 3: Wire LevelUpHandler into PlayerStatsProvider**

In `PlayerStatsProvider.java`, set the callback when providing the capability:

```java
// In getOrCreate(), after creating stats:
private PlayerStats getOrCreate() {
    if (this.stats == null) {
        this.stats = new PlayerStats();
        this.stats.setOnLevelUp((index, level) -> {
            // Will be connected via player reference
        });
    }
    return this.stats;
}
```

Since we need the ServerPlayer reference, better approach: in `CapabilityHandler` or wherever we access the capability with a player reference, set the callback:

In `CombatXPHandler` and `NonCombatXPHandler`, where we already have `ServerPlayer`, we can set the callback when first accessing stats.

Actually, the easiest approach: set the callback inside `LevelUpHandler` as a static method that gets called from `PlayerStats.addXp`. Since `PlayerStats` doesn't have a player reference, we need to restructure.

Better approach: In `ActionXpHelper.awardXp` and all direct stat access points, check for level-up and call `LevelUpHandler.onLevelUp`.

Actually simplest: in `PlayerStats.addXp`, after incrementing the level, return a boolean or signal. Or use a static listener map.

Let me use a simple cleaner approach: register a static listener in `PlayerStats`:

```java
// In PlayerStats.java
private static final java.util.Map<java.util.UUID, java.util.function.BiConsumer<Integer, Integer>> levelUpListeners = new java.util.HashMap<>();

public static void registerLevelUpListener(java.util.UUID playerUuid, java.util.function.BiConsumer<Integer, Integer> listener) {
    levelUpListeners.put(playerUuid, listener);
}

public static void unregisterLevelUpListener(java.util.UUID playerUuid) {
    levelUpListeners.remove(playerUuid);
}
```

And in `CapabilityHandler.onPlayerClone` and login, register the listener. When level up happens, call the listener.

Actually, the simplest approach that avoids complexity: just check for level-up in `ActionXpHelper.awardXp`:

```java
public static void awardXp(ServerPlayer player, int statIndex, XpTier tier) {
    player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
        int oldLevel = stats.getLevel(statIndex);
        int xp = tier.minXp + player.getRandom().nextInt(tier.maxXp - tier.minXp + 1);
        stats.addXp(statIndex, xp);
        int newLevel = stats.getLevel(statIndex);
        if (newLevel > oldLevel) {
            LevelUpHandler.onLevelUp(player, statIndex, newLevel);
        }
        NetworkHandler.sendToPlayer(
            new StatUpdatePacket(statIndex, newLevel, stats.getXp(statIndex)),
            player);
    });
}
```

- [ ] **Step 4: Update LevelUpToast to show milestone message**

In `LevelUpToast.java`, add milestone-specific messages:

```java
public static void onLevelUp(String statName, int newLevel) {
    String msg;
    if (newLevel == 100) {
        msg = "§6✦ " + statName + " → §eNIVEAU MAX §6✦";
    } else if (newLevel % 10 == 0) {
        msg = "§e★ " + statName + " → Palier " + newLevel + " ★";
    } else {
        msg = "§6" + statName + " → Level " + newLevel + "!";
    }
    show(msg);
}
```

- [ ] **Step 5: Build**

Run: `./gradlew build`

---

### Task 7: Perk system – data layer

**Files:**
- Create: `src/main/java/tong/statmod/perks/Perk.java`
- Create: `src/main/java/tong/statmod/perks/PerkManager.java`
- Create: `src/main/java/tong/statmod/perks/PerkProvider.java`
- Create: `src/main/java/tong/statmod/network/SyncPerksPacket.java`
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Modify: `src/main/java/tong/statmod/capability/CapabilityHandler.java`

- [ ] **Step 1: Define Perk enum**

```java
package tong.statmod.perks;

import tong.statmod.stats.StatType;

public enum Perk {
    // Force Brute
    BRUTE_BLOCK_DAMAGE(0, StatType.BRUTE_FORCE, 20, "Démolition", "Dégâts aux blocs +50%"),
    BRUTE_ARMOR_PIERCE(1, StatType.BRUTE_FORCE, 50, "Perce-Armure", "Dégâts aux armures +15%"),
    BRUTE_STUN(2, StatType.BRUTE_FORCE, 80, "Étourdissement", "Coups chargés étourdissent 1s"),

    // Blade Technique
    BLADE_PARRY_WINDOW(3, StatType.BLADE_TECHNIQUE, 20, "Parade Améliorée", "Fenêtre de parade +25%"),
    BLADE_COMBO_BONUS(4, StatType.BLADE_TECHNIQUE, 50, "Combo Mortel", "5 hits → prochain coup +50%"),
    BLADE_BLEED(5, StatType.BLADE_TECHNIQUE, 80, "Hémorragie", "Toucher 3 mobs inflige saignement"),

    // Rapidité
    RAPID_POST_ATTACK_SPEED(6, StatType.RAPIDITE, 20, "Élan", "+10% vitesse après attaque (2s)"),
    RAPID_DOUBLE_HIT(7, StatType.RAPIDITE, 50, "Double Frappe", "10% chance de double-hit"),
    RAPID_DODGE_BOOST(8, StatType.RAPIDITE, 80, "Instinct d'Esquive", "Esquiver → +50% vitesse attaque (3s)"),

    // Agility
    AGILITY_COMBAT_JUMP(9, StatType.AGILITY, 20, "Saut de Combat", "+20% hauteur saut en combat"),
    AGILITY_SPRINT_ATTACK(10, StatType.AGILITY, 50, "Course Percutante", "Sprint-saut inflige dégâts chute"),
    AGILITY_ELYTRA_SPEED(11, StatType.AGILITY, 80, "Vol de Combat", "Elytra +10% vitesse en combat"),

    // Physical Resistance
    RESIST_FALL(12, StatType.PHYSICAL_RESISTANCE, 20, "Atterrissage", "+10% résistance chute"),
    RESIST_ABSORB(13, StatType.PHYSICAL_RESISTANCE, 50, "Endurci", "Absorption 1 coeur après dégâts (10s cd)"),
    RESIST_TOUGHNESS(14, StatType.PHYSICAL_RESISTANCE, 80, "Cœur de Pierre", "30% chance de réduire dégâts de moitié"),

    // Physical Endurance
    ENDURANCE_MAGIC_BLOCK(15, StatType.PHYSICAL_ENDURANCE, 20, "Protection Totale", "Bloquer réduit aussi dégâts magiques 20%"),
    ENDURANCE_SPRINT_SHIELD(16, StatType.PHYSICAL_ENDURANCE, 50, "Rempart Mobile", "Sprint avec bouclier levé possible"),
    ENDURANCE_PERFECT_PARRY(17, StatType.PHYSICAL_ENDURANCE, 80, "Parade Parfaite", "Parer annule tous dégâts et repousse"),

    // Precision
    PRECISION_SNIPER(18, StatType.PRECISION, 20, "Sniper", "+10% dégâts à +15 blocs"),
    PRECISION_CRIT_KNOCKBACK(19, StatType.PRECISION, 50, "Tir Perforant", "Crit garantit knockback"),
    PRECISION_PIERCE(20, StatType.PRECISION, 80, "Perforation", "Tir traverse les mobs (+1 piercing)");

    // Survival, Crafting, Mental perks can be added following same pattern

    public final int id;
    public final StatType stat;
    public final int levelRequired;
    public final String name;
    public final String description;

    Perk(int id, StatType stat, int levelRequired, String name, String description) {
        this.id = id;
        this.stat = stat;
        this.levelRequired = levelRequired;
        this.name = name;
        this.description = description;
    }

    public static Perk byId(int id) {
        for (Perk p : values()) {
            if (p.id == id) return p;
        }
        return null;
    }
}
```

- [ ] **Step 2: Create PerkManager capability**

```java
package tong.statmod.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

public class PerkManager implements INBTSerializable<CompoundTag> {
    private final Set<Integer> unlockedPerks = new HashSet<>();
    private int availablePoints = 0;

    public boolean isUnlocked(Perk perk) { return unlockedPerks.contains(perk.id); }
    public Set<Integer> getUnlockedPerks() { return unlockedPerks; }
    public int getAvailablePoints() { return availablePoints; }
    public void addPoints(int amount) { this.availablePoints += amount; }

    public boolean unlockPerk(Perk perk) {
        if (availablePoints <= 0) return false;
        if (unlockedPerks.contains(perk.id)) return false;
        unlockedPerks.add(perk.id);
        availablePoints--;
        return true;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Points", availablePoints);
        ListTag list = new ListTag();
        for (int id : unlockedPerks) {
            list.add(IntTag.valueOf(id));
        }
        tag.put("UnlockedPerks", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.availablePoints = tag.getInt("Points");
        unlockedPerks.clear();
        ListTag list = tag.getList("UnlockedPerks", 3); // 3 = IntTag
        for (int i = 0; i < list.size(); i++) {
            unlockedPerks.add(list.getInt(i));
        }
    }
}
```

- [ ] **Step 3: Create PerkProvider**

```java
package tong.statmod.perks;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PerkProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PerkManager> PERKS = CapabilityManager.get(new CapabilityToken<>() {});

    private PerkManager manager;
    private final LazyOptional<PerkManager> lazyOptional = LazyOptional.of(this::getOrCreate);

    private PerkManager getOrCreate() {
        if (this.manager == null) this.manager = new PerkManager();
        return this.manager;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PERKS ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }
    @Override
    public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }
}
```

- [ ] **Step 4: Register capability and attach to players**

In `CapabilityHandler.java`, add:
```java
event.register(PerkManager.class);
```

And in `attachCapabilities`:
```java
event.addCapability(new ResourceLocation(STATMod.MODID, "perks"), new PerkProvider());
```

Also add clone handling:
```java
event.getOriginal().getCapability(PerkProvider.PERKS).ifPresent(oldPerks -> {
    event.getEntity().getCapability(PerkProvider.PERKS).ifPresent(newPerks -> {
        newPerks.deserializeNBT(oldPerks.serializeNBT());
    });
});
```

- [ ] **Step 5: Create SyncPerksPacket**

```java
package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientPerkCache;

import java.util.function.Supplier;

public class SyncPerksPacket {
    private final int[] unlockedPerkIds;
    private final int points;

    public SyncPerksPacket(int[] unlockedPerkIds, int points) {
        this.unlockedPerkIds = unlockedPerkIds;
        this.points = points;
    }

    public static void encode(SyncPerksPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.unlockedPerkIds);
        buf.writeInt(packet.points);
    }

    public static SyncPerksPacket decode(FriendlyByteBuf buf) {
        return new SyncPerksPacket(buf.readVarIntArray(), buf.readInt());
    }

    public static void handle(SyncPerksPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPerkCache.update(packet.unlockedPerkIds, packet.points));
        ctx.get().setPacketHandled(true);
    }
}
```

- [ ] **Step 6: Register packet in NetworkHandler**

```java
CHANNEL.registerMessage(packetId++, SyncPerksPacket.class,
    SyncPerksPacket::encode, SyncPerksPacket::decode, SyncPerksPacket::handle);
```

- [ ] **Step 7: Sync perks on login**

In `STATMod.LoginHandler.onPlayerLogin`, add:
```java
serverPlayer.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
    int[] ids = perks.getUnlockedPerks().stream().mapToInt(i -> i).toArray();
    NetworkHandler.sendToPlayer(new SyncPerksPacket(ids, perks.getAvailablePoints()), serverPlayer);
});
```

- [ ] **Step 8: Build**

Run: `./gradlew build`

---

### Task 8: Perk screen GUI

**Files:**
- Create: `src/main/java/tong/statmod/client/gui/PerkScreen.java`
- Create: `src/main/java/tong/statmod/client/ClientPerkCache.java`
- Modify: `src/main/java/tong/statmod/client/ClientSetup.java`

- [ ] **Step 1: Create ClientPerkCache**

```java
package tong.statmod.client;

import tong.statmod.perks.Perk;
import java.util.HashSet;
import java.util.Set;

public class ClientPerkCache {
    private static final Set<Integer> unlockedPerks = new HashSet<>();
    private static int availablePoints = 0;

    public static void update(int[] perkIds, int points) {
        unlockedPerks.clear();
        for (int id : perkIds) unlockedPerks.add(id);
        availablePoints = points;
    }

    public static boolean isUnlocked(Perk perk) { return unlockedPerks.contains(perk.id); }
    public static boolean isUnlocked(int perkId) { return unlockedPerks.contains(perkId); }
    public static int getAvailablePoints() { return availablePoints; }
}
```

- [ ] **Step 2: Create PerkScreen**

```java
package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatCategory;
import tong.statmod.stats.StatType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class PerkScreen extends Screen {
    private static final int TAB_COUNT = 5;
    private int selectedTab = 0;
    private final String[] tabNames = {"Combat", "Magie", "Survie", "Artisanat", "Mental"};
    private final StatCategory[] tabCategories = {
        StatCategory.COMBAT, StatCategory.MAGIC, StatCategory.SURVIVAL,
        StatCategory.CRAFTING, StatCategory.MENTAL};

    protected PerkScreen() {
        super(Component.translatable("screen.statmod.perks"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Title and points
        graphics.drawString(this.font, "§6Points disponibles: §e" + ClientPerkCache.getAvailablePoints(), 20, 10, 0xFFFFFF);

        // Tabs
        int tabX = 20;
        for (int i = 0; i < TAB_COUNT; i++) {
            int color = i == selectedTab ? 0xFFFFFF : 0x888888;
            graphics.drawString(this.font, tabNames[i], tabX, 25, color);
            tabX += this.font.width(tabNames[i]) + 15;
        }

        // Perks for current category
        StatCategory category = tabCategories[selectedTab];
        int y = 45;
        for (StatType stat : StatType.values()) {
            if (stat.category == category) {
                graphics.drawString(this.font, "§7" + stat.displayName + ":", 20, y, 0xAAAAAA);
                y += 12;
                for (Perk perk : Perk.values()) {
                    if (perk.stat == stat) {
                        boolean unlocked = ClientPerkCache.isUnlocked(perk);
                        boolean canUnlock = y + 20 > 0; // placeholder
                        String status = unlocked ? "§a[DÉBLOQUÉ]" : "§7[Niv." + perk.levelRequired + "]";
                        int color = unlocked ? 0x55FF55 : 0x888888;
                        graphics.drawString(this.font,
                            "  " + perk.name + " - " + perk.description + " " + status,
                            25, y, color);
                        y += 10;
                    }
                }
                y += 5;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabX = 20;
        for (int i = 0; i < TAB_COUNT; i++) {
            int tabWidth = this.font.width(tabNames[i]) + 15;
            if (mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= 25 && mouseY <= 40) {
                if (selectedTab != i) {
                    selectedTab = i;
                }
                return true;
            }
            tabX += tabWidth;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
```

- [ ] **Step 3: Add keybinding to open perk screen**

In `ClientSetup.java`:
```java
public static final KeyMapping OPEN_PERKS_KEY = new KeyMapping(
    "key.statmod.open_perks", InputConstants.KEY_O, "key.categories.statmod");
```

Register in `registerKeyMappings`:
```java
event.register(OPEN_PERKS_KEY);
```

Add handler in `ClientEventHandler`:
```java
if (OPEN_PERKS_KEY.consumeClick()) {
    Minecraft.getInstance().setScreen(new PerkScreen());
}
```

- [ ] **Step 4: Build**

Run: `./gradlew build`
