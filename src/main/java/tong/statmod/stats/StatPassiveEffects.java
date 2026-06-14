package tong.statmod.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;

import java.util.List;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class StatPassiveEffects {

    private static final int TICK_INTERVAL = 20;
    private static int tickCounter = 0;

    // Batched periodic effects: Keen Senses + Physical Endurance
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        CapabilityHelper.withStats(player, stats -> {
            // Keen Senses: reveal nearby mobs when sneaking
            int keenLevel = stats.getLevel(StatType.KEEN_SENSES.index);
            if (keenLevel > 0 && player.isShiftKeyDown()) {
                float range = StatCalculator.getDetectionRadius(keenLevel);
                AABB box = player.getBoundingBox().inflate(range);
                List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, box,
                    m -> m != null && m.isAlive() && m.hasLineOfSight(player));
                for (Mob mob : mobs) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }
            }

            // Physical Endurance: natural regen boost when hunger full
            int enduranceLevel = stats.getLevel(StatType.PHYSICAL_ENDURANCE.index);
            if (enduranceLevel > 0 && player.getFoodData().getFoodLevel() >= 18) {
                float health = player.getHealth();
                float maxHealth = player.getMaxHealth();
                if (health < maxHealth && health > 0) {
                    int delay = Math.max(8, 40 - enduranceLevel / 3);
                    if (player.tickCount % delay == 0) {
                        player.heal(0.5f);
                    }
                }
            }
        });
    }

    // Cooking: bonus nutrition + saturation when eating
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getItem();
        if (!result.getItem().isEdible()) return;

        FoodProperties food = result.getItem().getFoodProperties(result, player);
        if (food == null) return;

        CapabilityHelper.withStats(player, stats -> {
            int cookingLevel = stats.getLevel(StatType.COOKING.index);
            if (cookingLevel <= 0) return;

            float bonus = StatCalculator.getSaturationBonus(cookingLevel);
            int extraSat = Math.round(food.getNutrition() * bonus);
            int extraNut = StatCalculator.getExtraNutrition(cookingLevel);
            if (extraSat > 0 || extraNut > 0) {
                player.getFoodData().eat(extraNut, extraSat);
            }
            // Cooking also extends active food-related buff durations
            float buffDur = StatCalculator.getFoodBuffDuration(cookingLevel);
            if (buffDur > 1.0f) {
                var toExtend = new java.util.ArrayList<MobEffectInstance>();
                for (var effect : player.getActiveEffects()) {
                    if (effect.getEffect().isBeneficial()) {
                        toExtend.add(effect);
                    }
                }
                for (var effect : toExtend) {
                    int extended = Math.min((int)(effect.getDuration() * buffDur), 12000);
                    player.addEffect(new MobEffectInstance(
                        effect.getEffect(), extended,
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
                }
            }
        });
    }

    // Forging: mining speed bonus + durability preservation
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CapabilityHelper.withStats(player, stats -> {
            int forgingLevel = stats.getLevel(StatType.FORGING.index);
            if (forgingLevel <= 0) return;

            float bonus = StatCalculator.getDurabilityBonus(forgingLevel);
            event.setNewSpeed(event.getNewSpeed() * (1.0f + bonus));
        });
    }

    // Casting Speed: applied via StatEffectApplier as attack speed bonus
    // (item use speed requires complex Forge hooks — deferred)

    // Alchemy: short beneficial buff when drinking potions (scales with level)
    @SubscribeEvent
    public static void onPotionDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().is(Items.POTION)) return;

        CapabilityHelper.withStats(player, stats -> {
            int alchemyLevel = stats.getLevel(StatType.ALCHEMY.index);
            if (alchemyLevel <= 0) return;

            int duration = 100 + alchemyLevel * 2;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, false));
        });
    }

    // Water Affinity: +swim speed + extended breath when in water
    @SubscribeEvent
    public static void onSwimTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isInWater() && !player.isUnderWater()) return;
        if (player.tickCount % 10 != 0) return;

        CapabilityHelper.withStats(player, stats -> {
            int waterLevel = stats.getLevel(StatType.WATER_AFFINITY.index);
            if (waterLevel <= 0) return;
            float swimBonus = StatCalculator.getSwimSpeedBonus(waterLevel);
            if (swimBonus > 0) {
                player.setDeltaMovement(player.getDeltaMovement().add(
                    player.getLookAngle().x * swimBonus * 0.02,
                    0,
                    player.getLookAngle().z * swimBonus * 0.02
                ));
                // Extra oxygen: +15s at lvl 100
                int extraAir = (int) (waterLevel * 3);
                if (player.getAirSupply() < player.getMaxAirSupply() + extraAir) {
                    player.setAirSupply(player.getAirSupply() + 10);
                }
            }
        });
    }

    // Willpower: reduce status effect duration on apply
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CapabilityHelper.withStats(player, stats -> {
            int willpowerLevel = stats.getLevel(StatType.WILLPOWER.index);
            if (willpowerLevel <= 0) return;

            float reduction = StatCalculator.getStatusDurationReduction(willpowerLevel);
            if (reduction <= 0) return;

            MobEffectInstance effectInstance = event.getEffectInstance();
            if (effectInstance == null || effectInstance.isAmbient()) return;

            int reduced = (int) (effectInstance.getDuration() * (1.0f - reduction));
            if (reduced < 20) reduced = 20;

            player.removeEffect(effectInstance.getEffect());
            player.addEffect(new MobEffectInstance(
                effectInstance.getEffect(), reduced,
                effectInstance.getAmplifier(), effectInstance.isAmbient(), effectInstance.isVisible()));
        });
    }

    // Intimidation AoE: panic + weakness on mobs near the one you hit
    @SubscribeEvent
    public static void onLivingHurtAoE(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        CapabilityHelper.withStats(player, stats -> {
            int intimidationLevel = stats.getLevel(StatType.INTIMIDATION.index);
            if (intimidationLevel <= 0) return;

            float range = StatCalculator.getFearRange(intimidationLevel);
            AABB box = player.getBoundingBox().inflate(range);
            List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, box, m -> m.isAlive());
            for (Mob mob : mobs) {
                if (mob == event.getEntity()) continue;
                if (player.getRandom().nextFloat() < 0.3f) {
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 + intimidationLevel, 0));
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
            }
        });
    }
}