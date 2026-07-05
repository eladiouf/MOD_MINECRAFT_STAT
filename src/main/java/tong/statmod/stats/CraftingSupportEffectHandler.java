package tong.statmod.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkState;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class CraftingSupportEffectHandler {
    private static final int ETERNAL_ELIXIR_MIN_DURATION_TICKS = 24_000;
    private static final float BREW_DUPLICATE_CHANCE = 0.15f;
    private static final float FORGE_REPAIR_DUPLICATE_CHANCE = 0.15f;
    private static final float TRANSMUTATION_CHANCE = 0.10f;
    private static final long SHARPENING_WINDOW_MS = 30_000L;

    private CraftingSupportEffectHandler() {}

    public static int reducedDurabilityLoss(int baseLoss, int forgingLevel) {
        double multiplier = Math.max(0.0d, 1.0d - Math.max(0, forgingLevel) * 0.01d);
        return Math.max(0, (int) Math.round(baseLoss * multiplier));
    }

    public static float potionDurationMultiplier(int alchemyLevel) {
        return potionDurationMultiplier(alchemyLevel, false);
    }

    public static float potionDurationMultiplier(int alchemyLevel, boolean mixologistUnlocked) {
        return 1.0f + Math.max(0, alchemyLevel) * 0.01f + (mixologistUnlocked ? 0.10f : 0.0f);
    }

    public static float foodSaturationBonus(int cookingLevel) {
        return foodSaturationBonus(cookingLevel, false);
    }

    public static float foodSaturationBonus(int cookingLevel, boolean homeCookUnlocked) {
        return Math.max(0, cookingLevel) * 0.05f + (homeCookUnlocked ? 1.0f : 0.0f);
    }

    public static int potionAmplifier(int baseAmplifier, boolean philosopherStoneUnlocked) {
        int safeAmplifier = Math.max(0, baseAmplifier);
        if (!philosopherStoneUnlocked) {
            return Math.min(255, safeAmplifier);
        }
        int visibleLevel = safeAmplifier + 1;
        int boostedLevel = (int) Math.ceil(visibleLevel * 1.5d);
        return Math.min(255, Math.max(0, boostedLevel - 1));
    }

    public static int foodUseDurationTicks(int originalDurationTicks, boolean fastFoodUnlocked) {
        int duration = Math.max(1, originalDurationTicks);
        return fastFoodUnlocked ? Math.max(1, (int) Math.ceil(duration / 2.0d)) : duration;
    }

    public static int badFoodEffectDurationTicks(int originalDurationTicks, boolean ironStomachUnlocked) {
        int duration = Math.max(1, originalDurationTicks);
        return ironStomachUnlocked ? Math.max(1, (int) Math.ceil(duration / 2.0d)) : duration;
    }

    public static int potionEffectDurationTicks(int baseDurationTicks,
                                                int alchemyLevel,
                                                boolean mixologistUnlocked,
                                                boolean toxicologistUnlocked,
                                                boolean eternalElixirUnlocked,
                                                boolean poisonEffect) {
        int duration = Math.max(1, baseDurationTicks);
        float multiplier = potionDurationMultiplier(alchemyLevel, mixologistUnlocked);
        if (toxicologistUnlocked && poisonEffect) {
            multiplier *= 1.5f;
        }
        int scaled = Math.max(duration, Math.round(duration * multiplier));
        return eternalElixirUnlocked ? Math.max(scaled, ETERNAL_ELIXIR_MIN_DURATION_TICKS) : scaled;
    }

    public static int brewedPotionOutputCount(boolean brewersSecretUnlocked, float roll) {
        return brewersSecretUnlocked && roll < BREW_DUPLICATE_CHANCE ? 2 : 1;
    }

    public static long anvilLevelCost(long baseCost, boolean hammerHandUnlocked, boolean enchantingTouchUnlocked) {
        long cost = Math.max(1L, baseCost);
        if (hammerHandUnlocked) {
            cost = Math.max(1L, cost - 1L);
        }
        if (enchantingTouchUnlocked) {
            cost = Math.max(1L, Math.round(cost * 0.75d));
        }
        return cost;
    }

    public static int forgeRepairOutputCount(boolean masterSmithUnlocked, float roll) {
        return masterSmithUnlocked && roll < FORGE_REPAIR_DUPLICATE_CHANCE ? 2 : 1;
    }

    public static int perfectRepairDamageValue(int currentDamage, boolean creationUnlocked) {
        return creationUnlocked ? 0 : Math.max(0, currentDamage);
    }

    public static float sharpeningDamageMultiplier(boolean sharpeningUnlocked, boolean recentAnvilUse) {
        return sharpeningUnlocked && recentAnvilUse ? 1.10f : 1.0f;
    }

    public static int transmutationBonusCount(boolean transmutationUnlocked, float roll) {
        return transmutationUnlocked && roll < TRANSMUTATION_CHANCE ? 1 : 0;
    }

    @SubscribeEvent
    public static void onItemStart(LivingEntityUseItemEvent.Start event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack.getFoodProperties(player) == null) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        event.setDuration(foodUseDurationTicks(
                event.getDuration(),
                data.isPerkUnlocked(Perk.COOK_SITUATIONAL.id)));
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

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null || !isBadFoodStyleEffect(effect)) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (!data.isPerkUnlocked(Perk.COOK_ACTIVE.id)) {
            return;
        }
        effect.mapDuration(duration -> badFoodEffectDurationTicks(duration, true));
    }

    @SubscribeEvent
    public static void onPlayerBrewedPotion(PlayerBrewedPotionEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        ItemStack stack = event.getStack();
        if (!isPotionStack(stack)) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int outputCount = brewedPotionOutputCount(
                data.isPerkUnlocked(Perk.ALCHEM_ACTIVE.id),
                player.getRandom().nextFloat());
        if (outputCount <= 1) {
            return;
        }
        ItemStack bonus = stack.copy();
        bonus.setCount(outputCount - 1);
        if (!player.getInventory().add(bonus)) {
            player.drop(bonus, false);
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }

        PlayerStatData data = player.getData(ModAttachments.STATS);
        event.setCost(anvilLevelCost(
                event.getCost(),
                data.isPerkUnlocked(Perk.FORGE_CORE.id),
                data.isPerkUnlocked(Perk.FORGE_SYNERGY.id)));

        ItemStack output = event.getOutput();
        if (!output.isEmpty()
                && output.isDamageableItem()
                && data.isPerkUnlocked(Perk.FORGE_TRANSCENDENCE.id)) {
            ItemStack perfect = output.copy();
            perfect.setDamageValue(perfectRepairDamageValue(perfect.getDamageValue(), true));
            event.setOutput(perfect);
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        ItemStack output = event.getOutput();
        if (!output.isEmpty()
                && output.isDamageableItem()
                && data.isPerkUnlocked(Perk.FORGE_TRANSCENDENCE.id)) {
            output.setDamageValue(perfectRepairDamageValue(output.getDamageValue(), true));
        }

        int outputCount = forgeRepairOutputCount(
                data.isPerkUnlocked(Perk.FORGE_ACTIVE.id),
                player.getRandom().nextFloat());
        if (outputCount > 1 && !output.isEmpty()) {
            ItemStack bonus = output.copy();
            bonus.setCount(outputCount - 1);
            if (!player.getInventory().add(bonus)) {
                player.drop(bonus, false);
            }
        }

        if (data.isPerkUnlocked(Perk.FORGE_SITUATIONAL.id) && !output.isEmpty()) {
            PerkState.setCooldown(player.getUUID(), Perk.FORGE_SITUATIONAL.id, SHARPENING_WINDOW_MS);
        }
        if (data.isPerkUnlocked(Perk.FORGE_MASTERY.id)) {
            event.setBreakChance(event.getBreakChance() * 0.5f);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int bonusCount = transmutationBonusCount(
                data.isPerkUnlocked(Perk.ALCHEM_SYNERGY.id),
                player.getRandom().nextFloat());
        if (bonusCount <= 0) {
            return;
        }

        ItemStack bonus = new ItemStack(Items.GLOWSTONE_DUST, bonusCount);
        if (!player.getInventory().add(bonus)) {
            player.drop(bonus, false);
        }
    }

    private static void applyFoodBonus(Player player, ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int cooking = RaceEffectApplier.getEffectiveLevel(player, StatType.COOKING.index);
        float bonus = foodSaturationBonus(cooking, data.isPerkUnlocked(Perk.COOK_CORE.id));
        if (bonus > 0.0f) {
            player.getFoodData().eat(0, bonus);
        }
        if (data.isPerkUnlocked(Perk.COOK_TRANSCENDENCE.id)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, false, false));
        }
        if (data.isPerkUnlocked(Perk.COOK_MASTERY.id)) {
            shareFoodEffects(player);
        }
    }

    private static void applyPotionBonus(Player player, ItemStack stack) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int alchemy = RaceEffectApplier.getEffectiveLevel(player, StatType.ALCHEMY.index);
        boolean mixologist = data.isPerkUnlocked(Perk.ALCHEM_CORE.id);
        boolean toxicologist = data.isPerkUnlocked(Perk.ALCHEM_SITUATIONAL.id);
        boolean philosopherStone = data.isPerkUnlocked(Perk.ALCHEM_MASTERY.id);
        boolean eternalElixir = data.isPerkUnlocked(Perk.ALCHEM_TRANSCENDENCE.id);

        PotionContents potion = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance effect : potion.getAllEffects()) {
            if (effect.getDuration() <= 0) {
                continue;
            }
            int duration = potionEffectDurationTicks(
                    effect.getDuration(),
                    alchemy,
                    mixologist,
                    toxicologist,
                    eternalElixir,
                    isPoisonEffect(effect));
            int amplifier = potionAmplifier(effect.getAmplifier(), philosopherStone);
            player.addEffect(new MobEffectInstance(effect.getEffect(), duration, amplifier,
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }
    }

    private static boolean isPotionStack(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

    private static boolean isPoisonEffect(MobEffectInstance effect) {
        return effect.getEffect().equals(MobEffects.POISON);
    }

    private static boolean isBadFoodStyleEffect(MobEffectInstance effect) {
        return effect.getEffect().equals(MobEffects.HUNGER)
                || effect.getEffect().equals(MobEffects.POISON)
                || effect.getEffect().equals(MobEffects.CONFUSION);
    }

    private static void shareFoodEffects(Player player) {
        player.level().getEntitiesOfClass(Player.class,
                player.getBoundingBox().inflate(8),
                ally -> ally != player && ally.isAlive())
                .forEach(ally -> {
                    ally.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0, false, false));
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
                });
    }
}
