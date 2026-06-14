package tong.statmod.world.thirst;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.util.LagDetector;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.ThirstPacket;
import tong.statmod.sound.ModSounds;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatCalculator;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class ThirstHandler {
    private static final float HUNGER_MULTIPLIER = 1.0f;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        long __start = System.nanoTime();
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CapabilityHelper.withThirst(player, thirst -> {
            if (player.isCreative() || player.isSpectator()) return;

            float total = (float) Config.thirstBaseDecay;

            if (player.isSprinting()) {
                total += (float) Config.thirstSprintCost;
            }

            if (!player.onGround() && player.getDeltaMovement().y > 0.08) {
                total += (float) Config.thirstJumpCost;
            }

            for (ItemStack stack : player.getArmorSlots()) {
                if (!stack.isEmpty()) {
                    total += (float) Config.thirstArmorCostPerPiece;
                }
            }

            var biome = player.level().getBiome(player.blockPosition());
            if (biome.value().getBaseTemperature() > 1.0f) {
                total += (float) Config.thirstHotBiomeCost;
            }

            // Hunger multiplier (thirst decays faster when hungry)
            int foodLevel = player.getFoodData().getFoodLevel();
            if (foodLevel < 6) {
                total *= 2.0f;
            } else if (foodLevel < 10) {
                total *= 1.5f;
            }

            // Water Affinity reduces thirst decay
            float[] waterReduction = {0};
            CapabilityHelper.withStats(player, s -> {
                waterReduction[0] = Math.max(0.7f, 1.0f - s.getLevel(StatType.WATER_AFFINITY.index) * 0.003f);
            });
            total *= waterReduction[0];

            thirst.reduceThirst(total);

            // Dehydration effects
            float t = thirst.getThirst();
            if (t <= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false));
                player.level().playSound(null, player.blockPosition(),
                    ModSounds.THIRST_WARNING.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
                // 0.5 heart/sec damage every 20 ticks
                if (player.tickCount % 20 == 0) {
                    player.hurt(player.damageSources().starve(), 1.0f);
                }
            } else if (t < 10) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
                // Accelerated hunger
                if (player.tickCount % 40 == 0) {
                    player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - 1));
                }
            } else if (t < 25) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
            }

            syncIfChanged(player, thirst);
        });
        LagDetector.check("ThirstHandler", __start);
    }

    private static final java.util.Map<java.util.UUID, Float> lastThirstSync = new java.util.HashMap<>();

    private static void syncIfChanged(ServerPlayer player, ThirstManager thirst) {
        float current = thirst.getThirst();
        Float last = lastThirstSync.get(player.getUUID());
        if (last == null || Math.abs(current - last) > 3.0f) {
            NetworkHandler.sendToPlayer(new ThirstPacket(current), player);
            lastThirstSync.put(player.getUUID(), current);
        }
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastThirstSync.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        CapabilityHelper.withThirst(player, thirst -> {
            thirst.reduceThirst((float) Config.thirstBlockBreakCost);
            NetworkHandler.sendToPlayer(new ThirstPacket(thirst.getThirst()), player);
        });
    }
}
