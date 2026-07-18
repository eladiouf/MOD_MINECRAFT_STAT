package tong.statmod.dungeon.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

public final class SapperGoal extends Goal {
    private final Mob mob;
    private int cooldown;
    private int windup;
    private double x, y, z;

    public SapperGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = mob.getTarget();
        return target instanceof ServerPlayer && target.isAlive()
                && mob.distanceToSqr(target) >= 9.0 && mob.distanceToSqr(target) <= 32.0 * 32.0;
    }

    @Override public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        x = target.getX(); y = target.getY(); z = target.getZ();
        windup = 20;
        cooldown = 160;
        mob.level().playSound(null, mob.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.HOSTILE, 0.7F, 1.4F);
    }

    @Override public boolean canContinueToUse() { return windup > 0; }

    @Override public void tick() {
        if (!(mob.level() instanceof ServerLevel level) || windup-- <= 0) return;
        level.sendParticles(ParticleTypes.SMOKE, x, y + 0.1, z, 8, 1.5, 0.1, 1.5, 0.02);
        if (windup == 0) {
            level.sendParticles(ParticleTypes.FLAME, x, y + 0.1, z, 30, 2.0, 0.2, 2.0, 0.08);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(x - 2.5, y - 1, z - 2.5, x + 2.5, y + 2, z + 2.5))) {
                if (!player.isCreative() && !player.isSpectator()) {
                    player.hurt(mob.damageSources().mobAttack(mob), 8.0F);
                }
            }
        }
    }
}
