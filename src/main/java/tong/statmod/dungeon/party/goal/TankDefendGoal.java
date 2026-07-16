package tong.statmod.dungeon.party.goal;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import tong.statmod.dungeon.party.PartyRole;

import java.util.EnumSet;
import java.util.List;

public class TankDefendGoal extends Goal {

    private static final double PROTECT_RANGE = 18.0;
    private static final double CHARGE_DIST = 12.0;
    private static final double SHIELD_DIST = 4.0;

    private final Mob tank;
    private LivingEntity target;
    private int pathRecalcTimer;
    private int shieldTicks;
    private LivingEntity guardedAlly;

    public TankDefendGoal(Mob tank) {
        this.tank = tank;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        guardedAlly = findHealer();
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

        if (guardedAlly != null && guardedAlly.isAlive()
                && guardedAlly.hurtTime > 0
                && guardedAlly.distanceToSqr(tank) < PROTECT_RANGE * PROTECT_RANGE) {
            LivingEntity allyAttacker = guardedAlly.getLastHurtByMob();
            if (allyAttacker != null && allyAttacker.isAlive()
                    && guardedAlly.distanceToSqr(allyAttacker) < PROTECT_RANGE * PROTECT_RANGE) {
                target = allyAttacker;
            }
        }

        if (dist < SHIELD_DIST * SHIELD_DIST) {
            shieldTicks++;
            if (shieldTicks < 30) {
                if (!tank.isUsingItem()) {
                    tank.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                }
                knockbackEnemy();
            } else if (shieldTicks < 35) {
                if (tank.isUsingItem()) {
                    tank.stopUsingItem();
                }
                tank.doHurtTarget(target);
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            } else {
                shieldTicks = 0;
            }
        } else {
            if (tank.isUsingItem()) tank.stopUsingItem();
            shieldTicks = 0;
        }

        if (dist > CHARGE_DIST * CHARGE_DIST) {
            if (pathRecalcTimer-- <= 0) {
                tank.getNavigation().moveTo(target, 1.2);
                pathRecalcTimer = 10;
            }
            return;
        }

        if (guardedAlly != null && guardedAlly.isAlive()) {
            Vec3 toTarget = target.position().subtract(guardedAlly.position()).normalize();
            Vec3 intercept = guardedAlly.position().add(toTarget.scale(3));
            tank.getNavigation().moveTo(intercept.x, intercept.y, intercept.z, 1.0);
        } else {
            Vec3 groupCenter = findGroupCenter();
            if (groupCenter != null) {
                Vec3 toTarget = target.position().subtract(groupCenter).normalize();
                Vec3 interceptPos = groupCenter.add(toTarget.scale(4));
                tank.getNavigation().moveTo(interceptPos.x, interceptPos.y, interceptPos.z, 1.0);
            }
        }

        // Coup de mêlée dès que la cible est réellement à portée (la borne >= SHIELD_DIST²
        // rendait cette condition impossible : SHIELD_DIST²=16 > 3.5²=12.25 → code mort).
        if (dist < 3.5 * 3.5) {
            tank.doHurtTarget(target);
        }
    }

    @Override
    public void stop() {
        if (tank.isUsingItem()) tank.stopUsingItem();
        target = null;
        guardedAlly = null;
        shieldTicks = 0;
    }

    private void knockbackEnemy() {
        Vec3 pushDir = target.position().subtract(tank.position()).normalize();
        target.setDeltaMovement(
                pushDir.x * 1.5, 0.3, pushDir.z * 1.5);
        target.hurtMarked = true;
    }

    private LivingEntity findHealer() {
        List<Mob> allies = tank.level().getEntitiesOfClass(Mob.class,
                tank.getBoundingBox().inflate(PROTECT_RANGE),
                m -> m != tank && m.isAlive()
                        && m.getPersistentData().contains(PartyRole.TAG));

        for (Mob ally : allies) {
            if ("HEALER".equals(
                    ally.getPersistentData().getString(PartyRole.TAG))) {
                return ally;
            }
        }

        Mob closest = null;
        double minDist = Double.MAX_VALUE;
        for (Mob ally : allies) {
            double d = tank.distanceToSqr(ally);
            if (d < minDist) {
                minDist = d;
                closest = ally;
            }
        }
        return closest;
    }

    private LivingEntity findThreatNearAllies() {
        List<Mob> allies = tank.level().getEntitiesOfClass(Mob.class,
                tank.getBoundingBox().inflate(PROTECT_RANGE),
                m -> m != tank && m.isAlive()
                        && m.getPersistentData().contains(PartyRole.TAG));

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
                        && m.getPersistentData().contains(PartyRole.TAG));

        if (allies.isEmpty()) return null;

        double ax = 0, az = 0;
        for (Mob a : allies) {
            ax += a.getX();
            az += a.getZ();
        }
        return new Vec3(ax / allies.size(), tank.getY(), az / allies.size());
    }
}
