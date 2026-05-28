package tong.statmod.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;

import java.util.Iterator;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class StatPassiveEffects {

    // Cooking: bonus saturation when eating
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getItem();
        if (!result.getItem().isEdible()) return;

        FoodProperties food = result.getItem().getFoodProperties(result, player);
        if (food == null) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int cookingLevel = stats.getLevel(StatType.COOKING.index);
            if (cookingLevel <= 0) return;

            float bonus = StatCalculator.getSaturationBonus(cookingLevel);
            int extraSaturation = Math.round(food.getNutrition() * bonus);
            if (extraSaturation > 0) {
                player.getFoodData().eat(0, extraSaturation);
            }
        });
    }

    // Forging: chance to reduce durability loss when breaking a block/tool
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int forgingLevel = stats.getLevel(StatType.FORGING.index);
            if (forgingLevel <= 0) return;

            float bonus = StatCalculator.getDurabilityBonus(forgingLevel);
            event.setNewSpeed(event.getNewSpeed() * (1.0f + bonus * 0.5f));
        });
    }

    // Alchemy: extend potion duration when drinking
    @SubscribeEvent
    public static void onPotionDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().is(Items.POTION)) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int alchemyLevel = stats.getLevel(StatType.ALCHEMY.index);
            if (alchemyLevel <= 0) return;

            float extension = StatCalculator.getPotionDurationBonus(alchemyLevel);
            if (extension <= 0) return;

            for (Iterator<MobEffectInstance> it = player.getActiveEffectsMap().values().iterator(); it.hasNext();) {
                MobEffectInstance effect = it.next();
                if (effect.isAmbient()) continue;
                if (effect.getDuration() > 200) {
                    int extra = (int) (effect.getDuration() * extension);
                    player.addEffect(new MobEffectInstance(
                        effect.getEffect(), effect.getDuration() + extra,
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
                }
            }
        });
    }

    // Willpower: reduce status effect duration on apply
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int willpowerLevel = stats.getLevel(StatType.WILLPOWER.index);
            if (willpowerLevel <= 0) return;

            float reduction = StatCalculator.getStatusDurationReduction(willpowerLevel);
            if (reduction <= 0) return;

            MobEffectInstance effectInstance = event.getEffectInstance();
            if (effectInstance == null || effectInstance.isAmbient()) return;

            int reducedDuration = (int) (effectInstance.getDuration() * (1.0f - reduction));
            if (reducedDuration < 20) reducedDuration = 20;

            player.removeEffect(effectInstance.getEffect());
            player.addEffect(new MobEffectInstance(
                effectInstance.getEffect(), reducedDuration,
                effectInstance.getAmplifier(), effectInstance.isAmbient(), effectInstance.isVisible()));
        });
    }
}