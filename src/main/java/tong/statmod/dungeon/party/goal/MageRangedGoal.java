package tong.statmod.dungeon.party.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Le mage garde ses distances et invoque des crocs d'évocateur (Evoker Fangs)
 * sur sa cible. Recule si l'ennemi s'approche trop.
 */
public class MageRangedGoal extends Goal {

    private static final double PREFERRED_DIST = 12.0;
    private static final double FLEE_DIST = 5.0;
    private static final int CAST_INTERVAL = 60;

    private final Mob mage;
    private LivingEntity target;
    private int castCooldown;
    private int strafeTicks;

    public MageRangedGoal(Mob mage) {
        this.mage = mage;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = mage.getTarget();
        return target != null && target.isAlive()
                && mage.distanceToSqr(target) < 36 * 36;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = mage.distanceToSqr(target);

        // Fuit si l'ennemi est trop proche
        if (dist < FLEE_DIST * FLEE_DIST) {
            flee(target);
            return;
        }

        // Recule si l'ennemi s'approche de la zone de confort
        if (dist < PREFERRED_DIST * PREFERRED_DIST * 0.6) {
            Vec3 away = mage.position().subtract(target.position()).normalize();
            Vec3 dest = mage.position().add(away.scale(6));
            mage.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0);
        } else {
            mage.getNavigation().stop();
        }

        mage.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // Strafing latéral
        strafeTicks++;
        if (strafeTicks % 40 < 20) {
            Vec3 right = new Vec3(
                    target.getZ() - mage.getZ(), 0,
                    mage.getX() - target.getX()).normalize();
            Vec3 strafePos = mage.position().add(right.scale(2));
            mage.getNavigation().moveTo(strafePos.x, strafePos.y, strafePos.z, 0.8);
        }

        // Lancement du sort
        if (castCooldown <= 0) {
            castFangs(target);
            castCooldown = CAST_INTERVAL + mage.getRandom().nextInt(20);
        } else {
            castCooldown--;
        }
    }

    private void castFangs(LivingEntity target) {
        if (!(mage.level() instanceof ServerLevel level)) return;

        Vec3 tPos = target.position();
        for (int i = 0; i < 3; i++) {
            double ox = (mage.getRandom().nextDouble() - 0.5) * 3;
            double oz = (mage.getRandom().nextDouble() - 0.5) * 3;
            BlockPos fangPos = BlockPos.containing(tPos.x + ox, tPos.y, tPos.z + oz);

            var fangs = new net.minecraft.world.entity.projectile.EvokerFangs(
                    level, fangPos.getX() + 0.5, fangPos.getY(), fangPos.getZ() + 0.5,
                    (float) Math.atan2(oz, ox), 2, mage);
            level.addFreshEntity(fangs);
        }

        mage.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void flee(LivingEntity threat) {
        Vec3 away = mage.position().subtract(threat.position()).normalize();
        Vec3 fleePos = mage.position().add(away.scale(12));
        mage.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.3);
    }
}
