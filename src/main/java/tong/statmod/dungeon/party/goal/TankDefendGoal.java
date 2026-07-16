package tong.statmod.dungeon.party.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Le tank protège les membres du groupe : il cible en priorité les ennemis
 * qui s'en prennent à ses alliés, et se place entre l'ennemi et le groupe.
 */
public class TankDefendGoal extends Goal {

    private static final double PROTECT_RANGE = 18.0;
    private static final double CHARGE_DIST = 12.0;

    private final Mob tank;
    private LivingEntity target;
    private int pathRecalcTimer;

    public TankDefendGoal(Mob tank) {
        this.tank = tank;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Attaque soit la cible actuelle, soit l'ennemi le plus proche d'un allié
        target = tank.getTarget();
        if (target == null || !target.isAlive()) {
            target = findThreatNearAllies();
        }
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive()
                && tank.distanceToSqr(target) < 30 * 30;
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = tank.distanceToSqr(target);
        tank.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // Charge l'ennemi s'il est loin
        if (dist > CHARGE_DIST * CHARGE_DIST) {
            if (pathRecalcTimer-- <= 0) {
                tank.getNavigation().moveTo(target, 1.2);
                pathRecalcTimer = 10;
            }
            return;
        }

        // Se place entre l'ennemi et le groupe (position défensive)
        Vec3 groupCenter = findGroupCenter();
        if (groupCenter != null) {
            Vec3 toTarget = target.position().subtract(groupCenter).normalize();
            Vec3 interceptPos = groupCenter.add(toTarget.scale(4));
            tank.getNavigation().moveTo(interceptPos.x, interceptPos.y, interceptPos.z, 1.0);
        }

        // Attaque au corps à corps
        if (dist < 3.0 * 3.0) {
            tank.doHurtTarget(target);
        }
    }

    @Override
    public void stop() {
        target = null;
    }

    private LivingEntity findThreatNearAllies() {
        List<Mob> allies = tank.level().getEntitiesOfClass(Mob.class,
                tank.getBoundingBox().inflate(PROTECT_RANGE),
                m -> m != tank && m.isAlive()
                        && m.getPersistentData().contains(
                        tong.statmod.dungeon.party.PartyRole.TAG));

        for (Mob ally : allies) {
            LivingEntity allyTarget = ally.getTarget();
            if (allyTarget != null && allyTarget.isAlive()) {
                return allyTarget;
            }
        }
        return null;
    }

    private Vec3 findGroupCenter() {
        List<Mob> allies = tank.level().getEntitiesOfClass(Mob.class,
                tank.getBoundingBox().inflate(PROTECT_RANGE),
                m -> m != tank && m.isAlive()
                        && m.getPersistentData().contains(
                        tong.statmod.dungeon.party.PartyRole.TAG));

        if (allies.isEmpty()) return null;

        double ax = 0, az = 0;
        for (Mob a : allies) {
            ax += a.getX();
            az += a.getZ();
        }
        return new Vec3(ax / allies.size(), tank.getY(), az / allies.size());
    }
}
