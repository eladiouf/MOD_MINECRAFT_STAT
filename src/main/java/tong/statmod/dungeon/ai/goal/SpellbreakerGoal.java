package tong.statmod.dungeon.ai.goal;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public final class SpellbreakerGoal extends Goal {
    private final Mob mob;
    private ServerPlayer target;
    private int cooldown;

    public SpellbreakerGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        target = mob.level().getEntitiesOfClass(ServerPlayer.class,
                        mob.getBoundingBox().inflate(24.0),
                        player -> player.isAlive() && !player.isCreative()
                                && !player.isSpectator() && player.isUsingItem())
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        return target != null;
    }

    @Override public void start() {
        if (target == null) return;
        target.stopUsingItem();
        mob.setTarget(target);
        Vec3 push = target.position().subtract(mob.position());
        if (push.lengthSqr() > 0.01) {
            push = push.normalize().scale(1.1);
            target.push(push.x, 0.25, push.z);
        }
        cooldown = 100;
    }
}
