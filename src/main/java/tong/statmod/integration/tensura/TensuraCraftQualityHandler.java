package tong.statmod.integration.tensura;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class TensuraCraftQualityHandler {
    private TensuraCraftQualityHandler() {}

    public static int reducedDurabilityLoss(int baseLoss, int forgingLevel) {
        double multiplier = Math.max(0.2, 1.0 - Math.max(0, forgingLevel) * 0.01);
        return Math.max(0, (int) Math.round(baseLoss * multiplier));
    }

    public static float potionDurationMultiplier(int alchemyLevel) {
        return 1.0f + Math.max(0, alchemyLevel) * 0.01f;
    }

    public static float foodSaturationBonus(int cookingLevel) {
        return Math.max(0, cookingLevel) * 0.05f;
    }

    @SubscribeEvent
    public static void onItemFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItem();
        if (stack.getFoodProperties(player) != null) {
            applyFoodBonus(player, stack);
        } else if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            applyPotionBonus(player, stack);
        }
    }

    private static void applyFoodBonus(Player player, ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) {
            return;
        }
        int cooking = RaceEffectApplier.getEffectiveLevel(player, StatType.COOKING.index);
        float bonus = foodSaturationBonus(cooking);
        if (bonus <= 0.0f) {
            return;
        }
        player.getFoodData().eat(0, bonus);
    }

    private static void applyPotionBonus(Player player, ItemStack stack) {
        int alchemy = RaceEffectApplier.getEffectiveLevel(player, StatType.ALCHEMY.index);
        float multiplier = potionDurationMultiplier(alchemy);
        if (multiplier <= 1.0f) {
            return;
        }

        PotionContents potion = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance effect : potion.getAllEffects()) {
            if (effect.getDuration() <= 0) {
                continue;
            }
            int duration = Math.max(effect.getDuration(), (int) Math.round(effect.getDuration() * multiplier));
            player.addEffect(new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }
    }
}
