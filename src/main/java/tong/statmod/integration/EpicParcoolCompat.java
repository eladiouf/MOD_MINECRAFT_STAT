package tong.statmod.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.fatigue.FatigueManager;
import tong.statmod.fatigue.FatigueProvider;
import tong.statmod.network.FatiguePacket;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.ThirstPacket;
import tong.statmod.progression.ActionXpHelper;
import tong.statmod.stats.StatType;
import tong.statmod.world.thirst.ThirstManager;

/**
 * Integration layer between STAT Mod and Epic Parcool / ParCool.
 *
 * Features:
 * - Awards XP to relevant stats when players perform parkour actions
 * - Cooldown system to prevent XP spam
 * - Stat-based bonuses: Agility → speed, Physical Endurance → jump height
 * - Resource consumption: hunger, thirst, fatigue per action
 *
 * Stat mapping:
 *   Rapidité (2)           → speed-based actions (dodge)
 *   Agility (3)            → acrobatic actions (flip, roll, vault, wall jump, slide)
 *   Physical Endurance (5) → sustained effort (cling, hang, climb, wall run, crawl)
 */
public class EpicParcoolCompat {
    private static boolean loaded = false;

    // Cooldowns
    private static final int XP_COOLDOWN_TICKS = 20;       // 1 second between XP awards
    private static final int RESOURCE_COOLDOWN_TICKS = 60;  // 3 seconds between resource consumption

    // Attribute modifier UUIDs (deterministic for save/load)
    private static final UUID AGILITY_SPEED_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID ENDURANCE_JUMP_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    // --- Resource costs per action category ---
    // Tuned for 48-minute days with frequent parkour/combat.
    // Very low values: player does hundreds of actions per day.
    // Quick actions (dodge, flip, roll)
    private static final float QUICK_HUNGER_COST = 0.02f;
    private static final float QUICK_THIRST_COST = 0.01f;
    private static final float QUICK_FATIGUE_GAIN = 0.01f;

    // Standard actions (vault, wall jump, slide, cat leap)
    private static final float STANDARD_HUNGER_COST = 0.04f;
    private static final float STANDARD_THIRST_COST = 0.02f;
    private static final float STANDARD_FATIGUE_GAIN = 0.02f;

    // Sustained actions (cling, hang, climb, wall run, crawl)
    private static final float SUSTAINED_HUNGER_COST = 0.06f;
    private static final float SUSTAINED_THIRST_COST = 0.03f;
    private static final float SUSTAINED_FATIGUE_GAIN = 0.03f;

    public static void init() {
        loaded = ModList.get().isLoaded("epicparcool");
        // Always register tick handler (it checks loaded flag internally)
        MinecraftForge.EVENT_BUS.register(EpicParcoolCompat.class);
        if (loaded) {
            STATMod.LOGGER.info("Epic Parcool detected — STAT integration enabled");
            ParCoolHookRegistry.register();
        } else {
            STATMod.LOGGER.info("Epic Parcool not detected — STAT integration disabled");
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    static void cleanupPlayerCooldowns(UUID uuid) {
        if (!loaded) return;
        ParCoolHookRegistry.cleanupPlayer(uuid);
    }

    /**
     * Consume hunger, thirst and fatigue for a parkour action.
     */
    private static void consumeResources(ServerPlayer player, float hungerCost, float thirstCost, float fatigueGain) {
        // 1. Consume hunger — saturation first (float), then food level only when saturation is gone
        FoodData foodData = player.getFoodData();
        float sat = foodData.getSaturationLevel();
        if (sat > 0) {
            // Consume saturation first — it's a float, works with tiny values
            foodData.setSaturation(Math.max(0, sat - hungerCost));
        } else {
            // Only reduce food level (int) when saturation is fully depleted
            int currentFood = foodData.getFoodLevel();
            if (currentFood > 0) {
                foodData.setFoodLevel(Math.max(0, currentFood - 1));
            }
        }

        // 2. Consume thirst (STAT Mod) — float, works with tiny values
        CapabilityHelper.withThirst(player, thirst -> {
            thirst.reduceThirst(thirstCost);
            NetworkHandler.sendToPlayer(new ThirstPacket(thirst.getThirst()), player);
        });

        // 3. Increase fatigue (STAT Mod) — float, works with tiny values
        CapabilityHelper.withFatigue(player, fatigue -> {
            fatigue.addFatigue(fatigueGain);
            NetworkHandler.sendToPlayer(new FatiguePacket(fatigue.getFatigue(), fatigue.getMaxFatigue()), player);
        });
    }

    /**
     * Apply stat-based bonuses to a player for parkour.
     */
    public static void applyStatBonuses(ServerPlayer player) {
        CapabilityHelper.withStats(player, stats -> {
            int agility = stats.getLevel(StatType.AGILITY.index);
            int endurance = stats.getLevel(StatType.PHYSICAL_ENDURANCE.index);

            // Agility → movement speed bonus (up to +20% at level 100)
            applySpeedModifier(player, agility);

            // Physical Endurance → jump height bonus (up to +15% at level 100)
            applyJumpModifier(player, endurance);
        });
    }

    private static void applySpeedModifier(ServerPlayer player, int agility) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        speedAttr.removeModifier(AGILITY_SPEED_UUID);

        if (agility > 0) {
            double bonus = agility * 0.002; // +0.2% per level, +20% at 100
            speedAttr.addPermanentModifier(new AttributeModifier(
                AGILITY_SPEED_UUID, "statmod:agility_speed", bonus,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void applyJumpModifier(ServerPlayer player, int endurance) {
        AttributeInstance jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr == null) return;

        jumpAttr.removeModifier(ENDURANCE_JUMP_UUID);

        if (endurance > 0) {
            double bonus = endurance * 0.0015; // +0.15% per level, +15% at 100
            jumpAttr.addPermanentModifier(new AttributeModifier(
                ENDURANCE_JUMP_UUID, "statmod:endurance_jump", bonus,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // Inner class isolates ParCool references from class loading
    private static class ParCoolHookRegistry {
        private static final Map<String, Integer> xpCooldowns = new HashMap<>();
        private static final Map<String, Integer> resourceCooldowns = new HashMap<>();

        static void register() {
            MinecraftForge.EVENT_BUS.register(ParCoolHookRegistry.class);
        }

        @SubscribeEvent
        public static void onParCoolActionStart(com.alrex.parcool.api.unstable.action.ParCoolActionEvent.StartEvent event) {
            if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) return;

            Class<?> actionClass = event.getAction().getClass();
            String actionName = actionClass.getSimpleName();
            int currentTick = serverPlayer.tickCount;

            // Determine action category
            int xpStat = -1;
            ActionXpHelper.XpTier xpTier = ActionXpHelper.XpTier.COMMON;
            float hunger = 0, thirst = 0, fatigue = 0;

            switch (actionName) {
                case "Dodge" -> {
                    xpStat = StatType.RAPIDITE.index;
                    xpTier = ActionXpHelper.XpTier.COMMON;
                    hunger = QUICK_HUNGER_COST; thirst = QUICK_THIRST_COST; fatigue = QUICK_FATIGUE_GAIN;
                }
                case "CatLeap", "Flipping", "Roll", "Vault", "WallJump", "WallSlide", "Slide" -> {
                    xpStat = StatType.AGILITY.index;
                    xpTier = ActionXpHelper.XpTier.COMMON;
                    hunger = STANDARD_HUNGER_COST; thirst = STANDARD_THIRST_COST; fatigue = STANDARD_FATIGUE_GAIN;
                }
                case "ClingToCliff", "HangDown", "ClimbUp", "VerticalWallRun", "Crawl", "ChargeJump" -> {
                    xpStat = StatType.PHYSICAL_ENDURANCE.index;
                    xpTier = ActionXpHelper.XpTier.INTERMEDIATE;
                    hunger = SUSTAINED_HUNGER_COST; thirst = SUSTAINED_THIRST_COST; fatigue = SUSTAINED_FATIGUE_GAIN;
                }
                default -> { return; }
            }

            String xpKey = serverPlayer.getStringUUID() + ":xp:" + actionName;
            String resKey = serverPlayer.getStringUUID() + ":res";

            // XP cooldown (per action, 1s)
            Integer lastXp = xpCooldowns.get(xpKey);
            if (lastXp == null || (currentTick - lastXp) >= XP_COOLDOWN_TICKS) {
                xpCooldowns.put(xpKey, currentTick);
                ActionXpHelper.awardXp(serverPlayer, xpStat, xpTier);
            }

            // Resource cooldown (global per player, 3s)
            Integer lastRes = resourceCooldowns.get(resKey);
            if (lastRes == null || (currentTick - lastRes) >= RESOURCE_COOLDOWN_TICKS) {
                resourceCooldowns.put(resKey, currentTick);
                consumeResources(serverPlayer, hunger, thirst, fatigue);
            }
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            // Clean up stale cooldown entries every 5 seconds
            if (event.getServer().getTickCount() % 100 == 0) {
                int now = event.getServer().getTickCount();
                xpCooldowns.entrySet().removeIf(e -> now - e.getValue() > 200);
                resourceCooldowns.entrySet().removeIf(e -> now - e.getValue() > 200);
            }
        }

        static void cleanupPlayer(UUID uuid) {
            String prefix = uuid + ":";
            xpCooldowns.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
            resourceCooldowns.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        }
    }

    /**
     * Called every server tick to apply stat bonuses to players with ParCool.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;
        if (!loaded) return;

        // Only apply bonuses every 20 ticks (1 second) to reduce overhead
        if (serverPlayer.tickCount % 20 != 0) return;

        applyStatBonuses(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cleanupPlayerCooldowns(event.getEntity().getUUID());
    }
}
