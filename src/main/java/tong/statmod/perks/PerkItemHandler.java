package tong.statmod.perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class PerkItemHandler {

    // ==================== ANVIL ====================

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.FORGE_REPAIR) && event.getCost() > 0) {
                event.setCost(Math.max(1, (int) (event.getCost() * 0.8f)));
            }

            if (perks.isUnlocked(Perk.FORGE_TEMPLATE)) {
                ItemStack right = event.getRight();
                if (right.getItem() == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE
                    || (right.getItem().getClass().getSimpleName().contains("SmithingTemplate"))) {
                    if (event.getMaterialCost() > 0) {
                        event.setMaterialCost(0);
                    }
                }
            }
        });
    }

    // ==================== FOOD EATEN ====================

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.COOK_FEAST)) {
                FoodProperties food = event.getItem().getFoodProperties(player);
                if (food != null) {
                    float bonusSat = food.getSaturationModifier() * 0.2f;
                    player.getFoodData().setSaturation(
                        player.getFoodData().getSaturationLevel() + bonusSat);
                }
            }

            if (perks.isUnlocked(Perk.COOK_CHEF)) {
                FoodProperties food = event.getItem().getFoodProperties(player);
                if (food != null && food.getNutrition() >= 6) {
                    float bonusSat = food.getSaturationModifier() * 0.5f;
                    player.getFoodData().setSaturation(
                        player.getFoodData().getSaturationLevel() + bonusSat);
                }
            }
        });
    }

    // ==================== POTION CONSUMED ====================

    @SubscribeEvent
    public static void onPotionConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack item = event.getItem();

        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.ALCHEMY_BREWER) && item.getItem() == Items.POTION) {
                var effects = net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(item);
                for (var effect : effects) {
                    int extended = (int) (effect.getDuration() * 1.2f);
                    player.addEffect(new MobEffectInstance(
                        effect.getEffect(), extended, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()));
                }
            }
        });
    }

    // ==================== SMELTED ====================

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            ItemStack result = event.getSmelting();
            if (perks.isUnlocked(Perk.COOK_GRILL)) {
                FoodProperties food = result.getFoodProperties(player);
                if (food != null && food.isMeat()) {
                    if (player.getRandom().nextFloat() < 0.3f) {
                        player.getInventory().add(result.copy());
                    }
                }
            }
        });
    }

    // ==================== BREAK SPEED ====================

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.FORGE_NETHERITE)) {
                event.setNewSpeed(event.getNewSpeed() * 1.1f);
            }
        });
    }
}
