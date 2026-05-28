package tong.statmod.fatigue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.FatiguePacket;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.ThirstPacket;
import tong.statmod.stats.StatCalculator;
import tong.statmod.world.thirst.ThirstProvider;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class FatigueHandler {
    private static final java.util.Map<java.util.UUID, Boolean> wasOnGroundMap = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
            if (player.isSleeping()) {
                return;
            }

            int[] levels = {0, 0};
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(s -> {
                levels[0] = s.getLevel(5);
                levels[1] = s.getLevel(22);
            });
            int endurance = levels[0];
            int willpower = levels[1];

            // Set max fatigue capacity: base + endurance bonus
            fatigue.setMaxFatigue(Config.FATIGUE_MAX_CAPACITY.get() + endurance * 5);

            float enduranceMod = Math.max(0, 1.0f - endurance * 0.01f);
            float willpowerMod = Math.max(0, 1.0f - StatCalculator.getFatigueReduction(willpower));

            // Passive fatigue accumulation by environment
            float baseRate;
            if (!player.level().dimensionType().hasCeiling() && player.level().canSeeSky(player.blockPosition())) {
                long dayTime = player.level().getDayTime() % 24000;
                boolean isNight = dayTime > 13000 || dayTime < 1000;
                baseRate = (isNight ? Config.FATIGUE_NIGHT_RATE.get() : Config.FATIGUE_DAY_RATE.get()).floatValue();
            } else {
                baseRate = Config.FATIGUE_UNDERGROUND_RATE.get().floatValue();
            }
            fatigue.addFatigue(baseRate * enduranceMod);

            // Sprint cost (once per second, not per tick)
            if (player.isSprinting() && player.tickCount % 20 == 0) {
                fatigue.addFatigue(Config.FATIGUE_SPRINT_COST.get().floatValue() * willpowerMod);
                addThirstCost(player, 0.03f);
            }

            // Jump cost (once per jump, via wasOnGround tracking)
            boolean wasOnGround = wasOnGroundMap.getOrDefault(player.getUUID(), true);
            if (!player.onGround() && wasOnGround && player.getDeltaMovement().y > 0.0) {
                fatigue.addFatigue(Config.FATIGUE_JUMP_COST.get().floatValue() * willpowerMod);
                addThirstCost(player, 0.05f);
            }
            wasOnGroundMap.put(player.getUUID(), player.onGround());

            // Sneak recovery (only way to passively recover)
            if (player.isShiftKeyDown() && fatigue.getFatigue() > 0) {
                fatigue.reduceFatigue(Config.FATIGUE_SNEAK_RECOVERY.get().floatValue());
            }

            // Apply sleep deprivation penalty at dawn
            long dayTime = player.level().getDayTime() % 24000;
            if (dayTime < 100 && fatigue.getSleeplessNights() > 0) {
                if (fatigue.getLastSleepTime() < player.level().getGameTime() - 12000) {
                    applySleepPenalty(player, fatigue);
                }
            }

            syncIfChanged(player, fatigue);
        });
    }

    private static void applySleepPenalty(ServerPlayer player, FatigueManager fatigue) {
        int nights = fatigue.getSleeplessNights();
        float penalty = Config.FATIGUE_SLEEP_PENALTY_BASE.get().floatValue() * nights;
        // Willpower reduces penalty
        float[] adjusted = {penalty};
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int willpower = stats.getLevel(22);
            adjusted[0] = penalty * (1.0f - StatCalculator.getFatigueReduction(willpower));
        });
        fatigue.addFatigue(adjusted[0]);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.addFatigue(Config.FATIGUE_DAMAGE_COST.get().floatValue());
                syncIfChanged(player, fatigue);
            });
            addThirstCost(player, 1.0f);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.addFatigue(Config.FATIGUE_BLOCK_BREAK_COST.get().floatValue());
                syncIfChanged(player, fatigue);
            });
            addThirstCost(player, 0.3f);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(net.minecraftforge.event.entity.player.PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Only reset fatigue on natural wake-up (at dawn), not on interrupted sleep
        if (event.wakeImmediately()) return;

        player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
            fatigue.markSlept(player.level().getGameTime());
            syncIfChanged(player, fatigue);
        });
    }

    @SubscribeEvent
    public static void onDrinkWater(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItemStack().is(Items.POTION)) return;
        if (PotionUtils.getPotion(event.getItemStack()) != Potions.WATER) return;

        var thirstOpt = player.getCapability(ThirstProvider.THIRST).resolve();
        if (thirstOpt.isEmpty() || thirstOpt.get().isFull()) {
            event.setCanceled(true);
            return;
        }

        player.getCapability(ThirstProvider.THIRST).ifPresent(thirst -> {
            thirst.addThirst(8);
            NetworkHandler.sendToPlayer(new ThirstPacket(thirst.getThirst()), player);

            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                // Base petit, scale avec fatigue
                float base = 3.0f;
                float fatiguePercent = fatigue.getFatigue() / fatigue.getMaxFatigue();
                float multiplier = 1.0f + fatiguePercent * 2.0f;
                fatigue.reduceFatigue(base * multiplier);
                syncIfChanged(player, fatigue);
            });

            if (!player.isCreative()) {
                event.getItemStack().shrink(1);
                // Return glass bottle
                ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(glassBottle)) {
                    player.drop(glassBottle, false);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerFinishUse(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getItem();
        if (result.getItem().isEdible()) {
            FoodProperties food = result.getItem().getFoodProperties(result, player);
            if (food != null) {
                player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                    // Base: petit, scale avec la fatigue (plus fatigué = plus efficace)
                    float base = 1.0f + food.getNutrition() * 0.3f;
                    float fatiguePercent = fatigue.getFatigue() / fatigue.getMaxFatigue();
                    float multiplier = 1.0f + fatiguePercent * 2.0f; // x1 à faible fatigue, x3 à 100% fatigue
                    fatigue.reduceFatigue(base * multiplier);
                    syncIfChanged(player, fatigue);
                });
            }
        }
    }

    private static void addThirstCost(ServerPlayer player, float amount) {
        player.getCapability(ThirstProvider.THIRST).ifPresent(thirst -> {
            thirst.reduceThirst(amount);
            // Sync handled by ThirstHandler.syncIfChanged — don't send packet here
        });
    }

    private static final java.util.Map<java.util.UUID, Float> lastFatigueSync = new java.util.HashMap<>();

    private static void syncIfChanged(ServerPlayer player, FatigueManager fatigue) {
        float current = fatigue.getFatigue();
        Float last = lastFatigueSync.get(player.getUUID());
        if (last == null || Math.abs(current - last) > 3.0f) {
            NetworkHandler.sendToPlayer(new FatiguePacket(fatigue.getFatigue(), fatigue.getMaxFatigue()), player);
            lastFatigueSync.put(player.getUUID(), current);
        }
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastFatigueSync.remove(player.getUUID());
            wasOnGroundMap.remove(player.getUUID());
        }
    }
}
