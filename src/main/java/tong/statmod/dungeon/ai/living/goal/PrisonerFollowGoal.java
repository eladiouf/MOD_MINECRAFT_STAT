package tong.statmod.dungeon.ai.living.goal;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import tong.statmod.dungeon.ai.living.DungeonLivingActor;

public final class PrisonerFollowGoal extends Goal {
    private final Mob prisoner;
    private Player owner;

    public PrisonerFollowGoal(Mob prisoner) {
        this.prisoner = prisoner;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!prisoner.getPersistentData().getBoolean(DungeonLivingActor.PRISONER_RELEASED_TAG)
                || !(prisoner.level() instanceof ServerLevel level)) return false;
        try {
            UUID ownerId = UUID.fromString(prisoner.getPersistentData().getString(
                    DungeonLivingActor.PRISONER_OWNER_TAG));
            owner = level.getPlayerByUUID(ownerId);
            return owner != null && owner.isAlive() && prisoner.distanceToSqr(owner) < 48.0 * 48.0;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        if (owner == null) return;
        prisoner.getLookControl().setLookAt(owner, 25.0f, 25.0f);
        if (prisoner.distanceToSqr(owner) > 16.0) prisoner.getNavigation().moveTo(owner, 1.05);
        else prisoner.getNavigation().stop();
    }
}
