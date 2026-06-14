package tong.statmod.stats;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {
    private static final UUID ENDURANCE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID LUCK_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-defa-123456789abc");
    private static final UUID AIR_JUMP_UUID = UUID.fromString("f6a7b8c9-d0e1-2345-fabc-1234567890de");
    private static final UUID CAST_SPEED_UUID = UUID.fromString("a7b8c9d0-e1f2-3456-abcd-2345678901ef");
    private static final UUID MANA_SPEED_UUID = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // --- PLAYER ATTACKING ---
        if (event.getSource().getEntity() instanceof Player player) {
            final boolean[] critHolder = new boolean[1];
            CapabilityHelper.withStats(player, stats -> {
                float multiplier = 1.0f;

                // Brute Force + Blade Technique
                multiplier += StatCalculator.getDamageBonus(stats.getLevel(StatType.BRUTE_FORCE.index));
                multiplier += StatCalculator.getBladeDamageBonus(stats.getLevel(StatType.BLADE_TECHNIQUE.index));

                // Precision: critical hit
                float critChance = StatCalculator.getCritChance(stats.getLevel(StatType.PRECISION.index));
                boolean isCrit = player.getRandom().nextFloat() < critChance;
                critHolder[0] = isCrit;
                if (isCrit) multiplier *= 2.0f;

                // Precision: arrow damage bonus (bow/crossbow)
                if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
                    float arrowBonus = StatCalculator.getCritChance(stats.getLevel(StatType.PRECISION.index));
                    multiplier += arrowBonus;
                }

                event.setAmount(event.getAmount() * multiplier);

                // Arcane Power: amplify magic damage
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)) {
                    float arcaneBonus = StatCalculator.getMagicDamageBonus(stats.getLevel(StatType.ARCANE_POWER.index));
                    event.setAmount(event.getAmount() * (1.0f + arcaneBonus));
                }

                // Fire Affinity: amplify fire damage
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LAVA)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FIREBALL)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)) {
                    float fireBonus = StatCalculator.getFireDamageBonus(stats.getLevel(StatType.FIRE_AFFINITY.index));
                    event.setAmount(event.getAmount() * (1.0f + fireBonus));
                }

                // Intimidation: guaranteed debuff on direct hit
                LivingEntity target = event.getEntity();
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

                // Water Affinity: extra damage when underwater
                if (player.isInWater() || player.isUnderWater()) {
                    float waterDmg = StatCalculator.getUnderwaterDamageBonus(
                        stats.getLevel(StatType.WATER_AFFINITY.index));
                    if (waterDmg > 0) {
                        event.setAmount(event.getAmount() * (1.0f + waterDmg));
                    }
                }

                // Forging: bonus damage with tools (pickaxe, axe, shovel)
                var heldItem = player.getMainHandItem();
                boolean isTool = heldItem.is(net.minecraft.tags.ItemTags.PICKAXES)
                    || heldItem.is(net.minecraft.tags.ItemTags.AXES)
                    || heldItem.is(net.minecraft.tags.ItemTags.SHOVELS)
                    || heldItem.is(net.minecraft.tags.ItemTags.HOES);
                if (isTool) {
                    float toolDmg = StatCalculator.getToolDamageBonus(
                        stats.getLevel(StatType.FORGING.index));
                    if (toolDmg > 0) {
                        event.setAmount(event.getAmount() * (1.0f + toolDmg));
                    }
                }
            });
            // Combat feedback: floating damage numbers
            LivingEntity target = event.getEntity();
            final float finalDamage = event.getAmount();
            final boolean finalCrit = critHolder[0];
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> tong.statmod.client.feedback.CombatFeedbackRenderer.addDamageNumber(target, finalDamage, finalCrit));
        }

        // --- PLAYER BEING HIT ---
        if (event.getEntity() instanceof Player player) {
            // Keen Senses: dodge chance
            int[] keenLevel = {0};
            CapabilityHelper.withStats(player, s -> keenLevel[0] = s.getLevel(StatType.KEEN_SENSES.index));
            float dodgeChance = StatCalculator.getDodgeChance(keenLevel[0]);
            if (dodgeChance > 0 && player.getRandom().nextFloat() < dodgeChance) {
                event.setAmount(0);
                if (!player.level().isClientSide()) {
                    ServerLevel level = (ServerLevel) player.level();
                    level.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 1, player.getZ(),
                        8, 0.4, 0.4, 0.4, 0.02);
                }
                return;
            }

            CapabilityHelper.withStats(player, stats -> {
                // Mana Shield: absorbs magical damage
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.WITHER)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.DRAGON_BREATH)) {
                    float mana = stats.getMana();
                    if (mana > 0) {
                        float dmg = event.getAmount();
                        float absorbed = Math.min(mana, dmg);
                        stats.setMana(mana - absorbed);
                        event.setAmount(dmg - absorbed);
                        if (!player.level().isClientSide()) {
                            ServerLevel level = (ServerLevel) player.level();
                            level.sendParticles(ParticleTypes.END_ROD,
                                player.getX(), player.getY() + 1, player.getZ(),
                                6, 0.3, 0.5, 0.3, 0.03);
                        }
                    }
                }

                // Physical Resistance
                float reduction = StatCalculator.getDamageReduction(stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
                event.setAmount(event.getAmount() * (1.0f - reduction));

                // Air Affinity: reduce fall damage
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                    int airLevel = stats.getLevel(StatType.AIR_AFFINITY.index);
                    if (airLevel > 0) {
                        float fallReduction = StatCalculator.getJumpBonus(airLevel);
                        event.setAmount(event.getAmount() * (1.0f - fallReduction));
                    }
                }

                // Fire Affinity: reduce fire damage taken
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LAVA)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FIREBALL)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)) {
                    int fireLevel = stats.getLevel(StatType.FIRE_AFFINITY.index);
                    if (fireLevel > 0) {
                        float fireResist = StatCalculator.getFireDamageBonus(fireLevel) * 0.5f;
                        event.setAmount(event.getAmount() * (1.0f - fireResist));
                    }
                }

                // Magic Resistance
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.WITHER)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.DRAGON_BREATH)) {
                    float magicReduction = StatCalculator.getMagicReduction(stats.getLevel(StatType.MAGIC_RESISTANCE.index));
                    event.setAmount(event.getAmount() * (1.0f - magicReduction));
                }
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
        CapabilityHelper.withStats(player, stats -> {
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

            // Casting Speed → attack speed
            AttributeInstance castSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
            if (castSpeedAttr != null) {
                castSpeedAttr.removeModifier(CAST_SPEED_UUID);
                float castBonus = StatCalculator.getItemUseSpeed(stats.getLevel(StatType.CASTING_SPEED.index));
                if (castBonus > 0) {
                    castSpeedAttr.addPermanentModifier(new AttributeModifier(
                        CAST_SPEED_UUID, "Casting Speed Bonus", castBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Air Affinity → jump strength
            AttributeInstance jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpAttr != null) {
                jumpAttr.removeModifier(AIR_JUMP_UUID);
                float jumpBonus = StatCalculator.getJumpBonus(stats.getLevel(StatType.AIR_AFFINITY.index));
                if (jumpBonus > 0) {
                    jumpAttr.addPermanentModifier(new AttributeModifier(
                        AIR_JUMP_UUID, "Air Affinity Bonus", jumpBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Mana Sprint Boost: +10% speed when mana > 50%
            float mana = stats.getMana();
            float maxMana = stats.getMaxMana();
            if (maxMana > 0 && mana / maxMana > 0.5f) {
                AttributeInstance manaSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (manaSpeed != null) {
                    manaSpeed.removeModifier(MANA_SPEED_UUID);
                    manaSpeed.addPermanentModifier(new AttributeModifier(
                        MANA_SPEED_UUID, "Mana Sprint Boost", 0.1,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        });
    }

    // Erudition: multiply XP orbs dropped by mobs
    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player)) return;
        CapabilityHelper.withStats(player, stats -> {
            float xpBonus = StatCalculator.getXpBonus(stats.getLevel(StatType.ERUDITION.index));
            if (xpBonus > 0) {
                int extra = Math.round(event.getDroppedExperience() * xpBonus);
                event.setDroppedExperience(event.getDroppedExperience() + extra);
            }
        });
    }
}
