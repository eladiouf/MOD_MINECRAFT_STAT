package tong.statmod.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {
    private static final UUID ENDURANCE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID LUCK_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-defa-123456789abc");

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // --- PLAYER ATTACKING ---
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                float multiplier = 1.0f;

                // Brute Force + Blade Technique
                multiplier += StatCalculator.getDamageBonus(stats.getLevel(StatType.BRUTE_FORCE.index));
                multiplier += StatCalculator.getBladeDamageBonus(stats.getLevel(StatType.BLADE_TECHNIQUE.index));

                // Precision: critical hit
                float critChance = StatCalculator.getCritChance(stats.getLevel(StatType.PRECISION.index));
                boolean isCrit = player.getRandom().nextFloat() < critChance;
                if (isCrit) multiplier *= 2.0f;

                // Precision: arrow damage bonus (bow/crossbow)
                if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
                    float arrowBonus = StatCalculator.getCritChance(stats.getLevel(StatType.PRECISION.index));
                    multiplier += arrowBonus;
                }

                event.setAmount(event.getAmount() * multiplier);

                // Intimidation: guaranteed debuff on direct hit
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    int intimid = stats.getLevel(StatType.INTIMIDATION.index);
                    if (intimid > 0) {
                        int dur = 40 + intimid;
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WEAKNESS, dur, Math.min(2, intimid / 30)));
                        if (isCrit) {
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, dur / 2, 0));
                        }
                    }
                }
            });
        }

        // --- PLAYER BEING HIT ---
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                // Physical Resistance
                float reduction = StatCalculator.getDamageReduction(stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
                event.setAmount(event.getAmount() * (1.0f - reduction));
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAllBonuses(player);
        }
    }

    public static void applyAllBonuses(ServerPlayer player) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            // Physical Endurance → bonus hearts
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.removeModifier(ENDURANCE_UUID);
                float bonusHearts = StatCalculator.getEnduranceHearts(stats.getLevel(StatType.PHYSICAL_ENDURANCE.index));
                if (bonusHearts > 0) {
                    maxHealth.addPermanentModifier(new AttributeModifier(
                        ENDURANCE_UUID, "Endurance Bonus", bonusHearts, AttributeModifier.Operation.ADDITION));
                }
            }

            // Agility → move speed
            AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (moveSpeed != null) {
                moveSpeed.removeModifier(SPEED_UUID);
                float speedBonus = StatCalculator.getMoveSpeedBonus(stats.getLevel(StatType.AGILITY.index));
                if (speedBonus > 0) {
                    moveSpeed.addPermanentModifier(new AttributeModifier(
                        SPEED_UUID, "Agility Bonus", speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Rapidité → attack speed
            AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.removeModifier(ATTACK_SPEED_UUID);
                float atkSpeedBonus = StatCalculator.getAttackSpeedBonus(stats.getLevel(StatType.RAPIDITE.index));
                if (atkSpeedBonus > 0) {
                    attackSpeed.addPermanentModifier(new AttributeModifier(
                        ATTACK_SPEED_UUID, "Rapidité Bonus", atkSpeedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Tracking → luck
            AttributeInstance luck = player.getAttribute(Attributes.LUCK);
            if (luck != null) {
                luck.removeModifier(LUCK_UUID);
                float luckBonus = StatCalculator.getLuckBonus(stats.getLevel(StatType.TRACKING.index));
                if (luckBonus > 0) {
                    luck.addPermanentModifier(new AttributeModifier(
                        LUCK_UUID, "Tracking Bonus", luckBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        });
    }
}
