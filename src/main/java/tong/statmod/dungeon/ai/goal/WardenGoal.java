package tong.statmod.dungeon.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public final class WardenGoal extends Goal {
    private static final String X = "statmod_ai_anchor_x";
    private static final String Y = "statmod_ai_anchor_y";
    private static final String Z = "statmod_ai_anchor_z";
    private final Mob mob;

    public WardenGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
        if (!mob.getPersistentData().contains(X)) {
            mob.getPersistentData().putDouble(X, mob.getX());
            mob.getPersistentData().putDouble(Y, mob.getY());
            mob.getPersistentData().putDouble(Z, mob.getZ());
        }
    }

    @Override public boolean canUse() {
        return mob.distanceToSqr(mob.getPersistentData().getDouble(X),
                mob.getPersistentData().getDouble(Y), mob.getPersistentData().getDouble(Z)) > 14.0 * 14.0;
    }

    @Override public void start() {
        mob.getNavigation().moveTo(mob.getPersistentData().getDouble(X),
                mob.getPersistentData().getDouble(Y), mob.getPersistentData().getDouble(Z), 1.15);
    }

    @Override public boolean canContinueToUse() { return !mob.getNavigation().isDone(); }
}
