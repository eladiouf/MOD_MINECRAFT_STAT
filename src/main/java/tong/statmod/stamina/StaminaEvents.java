package tong.statmod.stamina;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

public final class StaminaEvents {
    private static final ResourceLocation MOVE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "low_stamina_move_speed");

    private StaminaEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        StaminaData data = player.getData(ModAttachments.STAMINA);
        if (shouldMeditate(player)) {
            MeditationManager.start(data);
        } else {
            MeditationManager.stop(data);
        }

        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        StaminaManager.tickPassive(data, endurance, data.meditating(), player.isSleeping(),
                player.isSprinting(), !player.onGround());
        applyThresholdMovementPenalty(player, data, endurance);
        if (StaminaRules.threshold(data.currentStamina(), StaminaRules.maxStamina(endurance)) == StaminaThreshold.CRITICAL) {
            player.setSprinting(false);
        }

        if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
            SyncHelper.syncStamina(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var food = event.getItem().getFoodProperties(player);
        if (food == null) {
            return;
        }

        var data = player.getData(ModAttachments.STAMINA);
        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        float fatigueRelief = StaminaRules.foodFatigueRelief(food.nutrition(), food.saturation());
        StaminaManager.restore(data,
                StaminaRules.foodRecoveryAmount(food.nutrition(), food.saturation()),
                endurance);
        StaminaManager.relieveFatigue(data, fatigueRelief);
        SyncHelper.syncStamina(player);
    }

    private static boolean shouldMeditate(Player player) {
        return player.onGround()
                && player.isShiftKeyDown()
                && !player.isSprinting()
                && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
    }

    private static void applyThresholdMovementPenalty(Player player, StaminaData data, int endurance) {
        var instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }

        instance.removeModifier(MOVE_SPEED_ID);
        StaminaThreshold threshold = StaminaRules.threshold(data.currentStamina(), StaminaRules.maxStamina(endurance));
        double amount = switch (threshold) {
            case LOW -> -0.10d;
            case CRITICAL -> -0.20d;
            default -> 0.0d;
        };
        if (amount != 0.0d) {
            instance.addTransientModifier(new AttributeModifier(
                    MOVE_SPEED_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
