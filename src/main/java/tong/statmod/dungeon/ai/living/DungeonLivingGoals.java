package tong.statmod.dungeon.ai.living;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import tong.statmod.dungeon.ai.living.goal.EngineerRepairGoal;
import tong.statmod.dungeon.ai.living.goal.PrisonerFollowGoal;
import tong.statmod.dungeon.ai.living.goal.ScavengerGoal;
import tong.statmod.dungeon.ai.living.goal.SurvivorGoal;

public final class DungeonLivingGoals {
    private DungeonLivingGoals() {}

    public static void ensureAttached(Mob actor) {
        DungeonLivingRole role = DungeonLivingActor.role(actor).orElse(null);
        if (role == null) return;
        Class<? extends Goal> type = switch (role) {
            case WOUNDED_SURVIVOR, WANDERING_MERCHANT -> SurvivorGoal.class;
            case PRISONER -> PrisonerFollowGoal.class;
            case SCAVENGER -> ScavengerGoal.class;
            case ENGINEER -> EngineerRepairGoal.class;
            default -> null;
        };
        if (type == null || actor.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> type.isInstance(wrapped.getGoal()))) return;
        Goal goal = switch (role) {
            case WOUNDED_SURVIVOR, WANDERING_MERCHANT -> new SurvivorGoal(actor);
            case PRISONER -> new PrisonerFollowGoal(actor);
            case SCAVENGER -> new ScavengerGoal(actor);
            default -> new EngineerRepairGoal(actor);
        };
        actor.goalSelector.addGoal(1, goal);
    }
}
