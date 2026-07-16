package tong.statmod.dungeon.party.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class AssassinAttackGoal extends Goal {

    private static final double FLANK_DIST = 2.5;
    private static final int FLANK_INTERVAL = 30;
    private static final double ATTACK_RANGE_SQ = 3.5 * 3.5;

    private final Mob assassin;
    private LivingEntity target;
    private int flankTimer;
    private int vanishCooldown;

    public AssassinAttackGoal(Mob assassin) {
        this.assassin = assassin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = assassin.getTarget();
        return target != null && target.isAlive()
                && assassin.distanceToSqr(target) < 20 * 20;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if (assassin.getEffect(MobEffects.MOVEMENT_SPEED) == null) {
            assassin.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, 6000, 1, false, false));
        }
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = assassin.distanceToSqr(target);
        assassin.getLookControl().setLookAt(target, 30.0f, 30.0f);

        --vanishCooldown;

        if (assassin.getHealth() < assassin.getMaxHealth() * 0.3
                && vanishCooldown <= 0) {
            vanishBehind();
            return;
        }

        flankTimer++;

        if (flankTimer % FLANK_INTERVAL == 0) {
            Vec3 behindPos = findBehindPosition();
            if (behindPos != null) {
                assassin.getNavigation().moveTo(
                        behindPos.x, behindPos.y, behindPos.z, 1.4);
            }
        }

        if (dist < ATTACK_RANGE_SQ) {
            if (isBehindTarget()) {
                target.hurt(assassin.damageSources().mobAttack(assassin), 16.0f);
                assassin.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

                Vec3 back = assassin.position().subtract(target.position()).normalize();
                Vec3 retreat = assassin.position().add(back.scale(4));
                assassin.getNavigation().moveTo(
                        retreat.x, retreat.y, retreat.z, 1.5);
                flankTimer = FLANK_INTERVAL - 8;
            } else if (isSideTarget()) {
                assassin.doHurtTarget(target);
                flankTimer = FLANK_INTERVAL - 3;
            }
        }
    }

    private boolean isBehindTarget() {
        Vec3 toAssassin = assassin.position().subtract(target.position()).normalize();
        Vec3 behindDir = target.getLookAngle().scale(-1).normalize();
        double angle = Math.toDegrees(Math.acos(
                Math.min(1.0, Math.max(-1.0, toAssassin.dot(behindDir)))));
        return angle < 60.0;
    }

    private boolean isSideTarget() {
        Vec3 toAssassin = assassin.position().subtract(target.position()).normalize();
        Vec3 lookVec = target.getLookAngle().normalize();
        double crossY = lookVec.x * toAssassin.z - lookVec.z * toAssassin.x;
        return Math.abs(crossY) > 0.5;
    }

    private Vec3 findBehindPosition() {
        if (target == null) return null;
        Vec3 behindDir = target.getLookAngle().normalize().scale(-FLANK_DIST);
        return target.position().add(behindDir.x, 0, behindDir.z);
    }

    private void vanishBehind() {
        if (!(assassin.level() instanceof ServerLevel level)) return;

        Vec3 behind = findBehindPosition();
        if (behind == null) return;

        BlockPos dest = BlockPos.containing(behind);
        for (int dy = 0; dy > -3; dy--) {
            BlockPos check = dest.offset(0, dy, 0);
            if (!level.getBlockState(check).isAir()) {
                dest = check.above();
                break;
            }
        }

        level.broadcastEntityEvent(assassin, (byte) 56);
        assassin.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);

        assassin.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);

        level.broadcastEntityEvent(assassin, (byte) 56);
        assassin.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);

        assassin.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY, 60, 0, false, false));

        if (assassin.getHealth() < assassin.getMaxHealth() * 0.3) {
            assassin.heal(8.0f);
        }

        vanishCooldown = 200;
        flankTimer = 0;
        assassin.getNavigation().stop();
    }
}
