package tong.statmod.world.thirst;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.ThirstPacket;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class ThirstHandler {
    private static final float BASE_DECAY = 0.003f;
    private static final float SPRINT_COST = 0.05f;
    private static final float JUMP_COST = 0.1f;
    private static final float BLOCK_BREAK_COST = 0.5f;
    private static final float ARMOR_COST_PER_PIECE = 0.005f;
    private static final float HOT_BIOME_COST = 0.005f;
    private static final float HUNGER_MULTIPLIER = 1.0f;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CapabilityHelper.withThirst(player, thirst -> {
            if (player.isCreative() || player.isSpectator()) return;

            float total = BASE_DECAY;

            // Sprint cost
            if (player.isSprinting()) {
                total += SPRINT_COST;
            }

            // Jump cost
            if (!player.onGround() && player.getDeltaMovement().y > 0.08) {
                total += JUMP_COST;
            }

            // Armor cost (per armor piece worn)
            for (ItemStack stack : player.getArmorSlots()) {
                if (!stack.isEmpty()) {
                    total += ARMOR_COST_PER_PIECE;
                }
            }

            // Hot biome cost (temperature > 1.0 = desert, badlands, nether, savanna)
            var biome = player.level().getBiome(player.blockPosition());
            if (biome.value().getBaseTemperature() > 1.0f) {
                total += HOT_BIOME_COST;
            }

            // Hunger multiplier (thirst decays faster when hungry)
            int foodLevel = player.getFoodData().getFoodLevel();
            if (foodLevel < 6) {
                total *= 2.0f;
            } else if (foodLevel < 10) {
                total *= 1.5f;
            }

            thirst.reduceThirst(total);

            // Dehydration effects
            float t = thirst.getThirst();
            if (t <= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false));
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
            thirst.reduceThirst(BLOCK_BREAK_COST);
            NetworkHandler.sendToPlayer(new ThirstPacket(thirst.getThirst()), player);
        });
    }
}
