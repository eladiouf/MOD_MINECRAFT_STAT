package tong.statmod.progression.xp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;

public final class CombatEligibility {
    private CombatEligibility() {
    }

    public static boolean eligibleTarget(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || target == player
                || !target.isAlive() || target.isInvulnerable()
                || target instanceof ArmorStand || player.isAlliedTo(target)) {
            return false;
        }
        return !(target instanceof TamableAnimal tamable
                && player.getUUID().equals(tamable.getOwnerUUID()));
    }

    public static boolean physicalProfile(DamageSource source, ServerPlayer victim) {
        if (source == null || victim == null) {
            return false;
        }
        var responsible = source.getEntity();
        return isPhysical(new CombatDamageProfile(
                source.is(DamageTypeTags.IS_FIRE),
                source.is(DamageTypeTags.BYPASSES_ARMOR),
                source.is(DamageTypeTags.IS_DROWNING),
                source.is(DamageTypeTags.IS_FALL),
                source.is(DamageTypeTags.IS_FREEZING),
                source.is(DamageTypeTags.IS_LIGHTNING),
                responsible == victim,
                responsible != null,
                source.is(DamageTypeTags.IS_PROJECTILE),
                source.is(DamageTypeTags.IS_EXPLOSION)));
    }

    public static boolean isPhysical(CombatDamageProfile profile) {
        return profile != null
                && !profile.fire()
                && !profile.bypassesArmor()
                && !profile.drowning()
                && !profile.fall()
                && !profile.freezing()
                && !profile.lightning()
                && !profile.selfInflicted()
                && profile.responsibleEntity();
    }

    public record CombatDamageProfile(
            boolean fire,
            boolean bypassesArmor,
            boolean drowning,
            boolean fall,
            boolean freezing,
            boolean lightning,
            boolean selfInflicted,
            boolean responsibleEntity,
            boolean projectile,
            boolean explosion) {
    }
}
