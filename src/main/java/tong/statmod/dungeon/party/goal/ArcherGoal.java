package tong.statmod.dungeon.party.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Archer / rôdeur du groupe : maintient la distance, tire des flèches (en code, anticipées), et
 * <b>marque sa proie</b> (Glowing) pour focaliser le groupe. Recule quand on l'approche.
 */
public class ArcherGoal extends Goal {

    private static final double PREFERRED = 14.0;
    private static final double FLEE = 6.0;
    private static final int SHOOT_INTERVAL = 35;
    private static final int MARK_INTERVAL = 120;

    private final Mob archer;
    private LivingEntity target;
    private int shootCooldown;
    private int markCooldown;
    private int strafe;

    public ArcherGoal(Mob archer) {
        this.archer = archer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = archer.getTarget();
        return target != null && target.isAlive() && archer.distanceToSqr(target) < 50 * 50;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = archer.distanceToSqr(target);
        archer.getLookControl().setLookAt(target, 30.0f, 30.0f);

        if (dist < FLEE * FLEE) {
            Vec3 away = archer.position().subtract(target.position()).normalize();
            Vec3 dest = archer.position().add(away.scale(8));
            archer.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.25);
        } else if (dist > PREFERRED * PREFERRED) {
            Vec3 toward = target.position().subtract(archer.position()).normalize();
            Vec3 dest = archer.position().add(toward.scale(4));
            archer.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.1);
        } else {
            // À bonne distance : strafe pour esquiver.
            strafe++;
            Vec3 side = new Vec3(target.getZ() - archer.getZ(), 0, archer.getX() - target.getX())
                    .normalize().scale(strafe % 80 < 40 ? 3 : -3);
            Vec3 dest = archer.position().add(side);
            archer.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.9);
        }

        if (markCooldown > 0) markCooldown--;
        else {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
            markCooldown = MARK_INTERVAL;
        }

        if (shootCooldown > 0) shootCooldown--;
        else if (dist < 40 * 40) {
            shootArrow();
            shootCooldown = SHOOT_INTERVAL + archer.getRandom().nextInt(15);
        }
    }

    private void shootArrow() {
        if (!(archer.level() instanceof ServerLevel level)) return;
        Arrow arrow = new Arrow(level, archer);
        Vec3 tPos = target.position().add(0, target.getBbHeight() * 0.4, 0);
        Vec3 tVel = target.getDeltaMovement();
        double lead = archer.distanceTo(target) / 3.0;
        Vec3 predicted = tPos.add(tVel.scale(Math.min(lead, 5.0)));
        double dx = predicted.x - archer.getX();
        double dy = predicted.y - archer.getEyeY();
        double dz = predicted.z - archer.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        arrow.setPos(archer.getX(), archer.getEyeY(), archer.getZ());
        arrow.setBaseDamage(3.0 + archer.getMaxHealth() * 0.02);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(dx, dy + horiz * 0.06, dz, 2.2f, 1.0f);
        archer.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        archer.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.1f);
        level.addFreshEntity(arrow);
    }
}
