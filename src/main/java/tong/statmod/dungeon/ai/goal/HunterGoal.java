package tong.statmod.dungeon.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import tong.statmod.dungeon.ai.DungeonAiActor;

public final class HunterGoal extends Goal {
    private final Mob mob;

    public HunterGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        long seen = mob.getPersistentData().getLong(DungeonAiActor.LAST_SEEN_TICK);
        return mob.getTarget() == null && seen > 0
                && mob.level().getGameTime() - seen <= 100
                && mob.getNavigation().isDone();
    }

    @Override public void start() {
        mob.getNavigation().moveTo(
                mob.getPersistentData().getDouble(DungeonAiActor.LAST_SEEN_X),
                mob.getPersistentData().getDouble(DungeonAiActor.LAST_SEEN_Y),
                mob.getPersistentData().getDouble(DungeonAiActor.LAST_SEEN_Z), 1.2);
    }

    @Override public boolean canContinueToUse() { return !mob.getNavigation().isDone(); }
}
