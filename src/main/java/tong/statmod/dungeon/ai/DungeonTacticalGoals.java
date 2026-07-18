package tong.statmod.dungeon.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import tong.statmod.dungeon.ai.goal.BerserkerGoal;
import tong.statmod.dungeon.ai.goal.HunterGoal;
import tong.statmod.dungeon.ai.goal.SapperGoal;
import tong.statmod.dungeon.ai.goal.ScoutGoal;
import tong.statmod.dungeon.ai.goal.SpellbreakerGoal;
import tong.statmod.dungeon.ai.goal.WardenGoal;

public final class DungeonTacticalGoals {
    private DungeonTacticalGoals() {}

    public static void ensureAttached(Mob mob) {
        DungeonTacticalRole role = DungeonAiActor.tacticalRole(mob);
        Class<? extends Goal> goalClass = switch (role) {
            case SCOUT, AMBUSHER -> ScoutGoal.class;
            case BERSERKER -> BerserkerGoal.class;
            case SPELLBREAKER, HEXER -> SpellbreakerGoal.class;
            case HUNTER -> HunterGoal.class;
            case SAPPER, ARCANE_ARTILLERY -> SapperGoal.class;
            case SHIELD_CAPTAIN, SPEAR_KEEPER, WARDEN, JAILER -> WardenGoal.class;
            default -> null;
        };
        if (goalClass == null || mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalClass.isInstance(wrapped.getGoal()))) return;
        Goal goal = switch (role) {
            case SCOUT, AMBUSHER -> new ScoutGoal(mob);
            case BERSERKER -> new BerserkerGoal(mob);
            case SPELLBREAKER, HEXER -> new SpellbreakerGoal(mob);
            case HUNTER -> new HunterGoal(mob);
            case SAPPER, ARCANE_ARTILLERY -> new SapperGoal(mob);
            default -> new WardenGoal(mob);
        };
        mob.goalSelector.addGoal(1, goal);
    }
}
