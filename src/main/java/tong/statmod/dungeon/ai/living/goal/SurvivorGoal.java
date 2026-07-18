package tong.statmod.dungeon.ai.living.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tong.statmod.dungeon.ai.living.DungeonLivingActor;

/** Keeps survivors and merchants near rescuers while fleeing active combat mobs. */
public final class SurvivorGoal extends Goal {
    private final Mob actor;

    public SurvivorGoal(Mob actor) {
        this.actor = actor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() { return actor.isAlive(); }
    @Override public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        Mob threat = actor.level().getEntitiesOfClass(Mob.class,
                actor.getBoundingBox().inflate(8.0), mob -> mob != actor && mob.isAlive()
                        && !mob.getPersistentData().getBoolean(DungeonLivingActor.NON_COMBAT_TAG))
                .stream().min(java.util.Comparator.comparingDouble(actor::distanceToSqr))
                .orElse(null);
        if (threat != null) {
            Vec3 away = actor.position().subtract(threat.position()).normalize();
            Vec3 destination = actor.position().add(away.scale(7.0));
            actor.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.25);
            return;
        }
        Player rescuer = actor.level().getEntitiesOfClass(Player.class,
                actor.getBoundingBox().inflate(16.0), player -> player.isAlive()
                        && !player.isCreative() && !player.isSpectator()).stream()
                .min(java.util.Comparator.comparingDouble(actor::distanceToSqr)).orElse(null);
        if (rescuer == null) return;
        actor.getLookControl().setLookAt(rescuer, 20.0f, 20.0f);
        if (actor.distanceToSqr(rescuer) > 25.0) actor.getNavigation().moveTo(rescuer, 0.9);
        else actor.getNavigation().stop();
    }
}
