package tong.statmod.dungeon.ai.living.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

/** Approaches battlefield remains for ambience but never consumes or deletes the item entity. */
public final class ScavengerGoal extends Goal {
    private final Mob scavenger;
    private ItemEntity interest;
    private int rescan;

    public ScavengerGoal(Mob scavenger) {
        this.scavenger = scavenger;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (rescan-- > 0 && interest != null && interest.isAlive()) return true;
        rescan = 20;
        interest = scavenger.level().getEntitiesOfClass(ItemEntity.class,
                scavenger.getBoundingBox().inflate(12.0), item -> item.isAlive()
                        && !item.getItem().isEmpty()).stream()
                .min(java.util.Comparator.comparingDouble(scavenger::distanceToSqr)).orElse(null);
        return interest != null;
    }

    @Override public boolean canContinueToUse() { return interest != null && interest.isAlive(); }

    @Override
    public void tick() {
        scavenger.getLookControl().setLookAt(interest, 20.0f, 20.0f);
        if (scavenger.distanceToSqr(interest) > 4.0) scavenger.getNavigation().moveTo(interest, 0.9);
        else scavenger.getNavigation().stop();
    }
}
