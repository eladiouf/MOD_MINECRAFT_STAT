package tong.statmod.dungeon.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import tong.statmod.dungeon.ai.DungeonAiActor;
import tong.statmod.dungeon.ai.DungeonAlertState;

public final class ScoutGoal extends Goal {
    private final Mob mob;
    private int cooldown;

    public ScoutGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive()
                && DungeonAiActor.alert(mob) == DungeonAlertState.COMBAT;
    }

    @Override public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        Vec3 away = mob.position().subtract(target.position());
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        away = away.normalize().scale(10.0);
        mob.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.25);
        cooldown = 80;
    }

    @Override public boolean canContinueToUse() { return !mob.getNavigation().isDone(); }
}
