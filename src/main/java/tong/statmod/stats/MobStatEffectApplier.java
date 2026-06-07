package tong.statmod.stats;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;

import java.util.UUID;

public class MobStatEffectApplier {

    private static final UUID MOB_STAT_UUID =
        UUID.fromString("9a8b7c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d");

    public static void applyAllBonuses(Mob mob) {
        mob.getCapability(MobStatsProvider.MOB_STATS).ifPresent(stats -> {
            applyDamage(mob, stats);
            applyHealth(mob, stats);
            applySpeed(mob, stats);
            applyResistance(mob, stats);
            applyFollowRange(mob, stats);
            applyKnockbackResistance(mob, stats);
        });
    }

    private static void applyDamage(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getDamageBonusMultiplier(
            stats.getLevel(StatType.BRUTE_FORCE.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_damage",
                bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void applyHealth(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getHealthBonusFlat(
            stats.getLevel(StatType.PHYSICAL_ENDURANCE.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_health",
                bonus, AttributeModifier.Operation.ADDITION));
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private static void applySpeed(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        int combinedLevel = stats.getLevel(StatType.AGILITY.index)
                          + stats.getLevel(StatType.RAPIDITE.index);
        float bonus = MobStatCalculator.getSpeedBonus(combinedLevel / 2);
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_speed",
                bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void applyResistance(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.ARMOR);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float reduction = MobStatCalculator.getDamageReduction(
            stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
        if (reduction > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_armor",
                reduction * 20.0, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void applyFollowRange(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getFollowRangeBonus(
            stats.getLevel(StatType.TRACKING.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_follow_range",
                bonus, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void applyKnockbackResistance(Mob mob, MobStats stats) {
        var attr = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr == null) return;
        attr.removeModifier(MOB_STAT_UUID);
        float bonus = MobStatCalculator.getKnockbackResistance(
            stats.getLevel(StatType.WILLPOWER.index));
        if (bonus > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MOB_STAT_UUID, "statmod:mob_knockback_res",
                bonus, AttributeModifier.Operation.ADDITION));
        }
    }
}
