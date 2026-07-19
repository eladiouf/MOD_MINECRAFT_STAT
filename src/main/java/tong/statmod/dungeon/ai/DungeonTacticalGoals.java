package tong.statmod.dungeon.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import tong.statmod.dungeon.ai.goal.BerserkerGoal;
import tong.statmod.dungeon.ai.goal.DungeonSpellcasterGoal;
import tong.statmod.dungeon.ai.goal.HunterGoal;
import tong.statmod.dungeon.ai.goal.SapperGoal;
import tong.statmod.dungeon.ai.goal.ScoutGoal;
import tong.statmod.dungeon.ai.goal.SpellbreakerGoal;
import tong.statmod.dungeon.ai.goal.WardenGoal;
import tong.statmod.dungeon.party.PartyRole;

public final class DungeonTacticalGoals {
    private DungeonTacticalGoals() {}

    public static void ensureAttached(Mob mob) {
        DungeonTacticalRole role = DungeonAiActor.tacticalRole(mob);
        boolean partyCaster = mob.getPersistentData().contains(PartyRole.TAG)
                && switch (role) {
                    case ELEMENTAL_CASTER, BATTLE_CLERIC, NECROMANCER, HEXER,
                            ARCANE_ARTILLERY -> true;
                    default -> false;
                };
        if (partyCaster) return;
        Class<? extends Goal> goalClass = switch (role) {
            case SCOUT, AMBUSHER -> ScoutGoal.class;
            case BERSERKER -> BerserkerGoal.class;
            case SPELLBREAKER -> SpellbreakerGoal.class;
            case HUNTER -> HunterGoal.class;
            case SAPPER -> SapperGoal.class;
            case SHIELD_CAPTAIN, SPEAR_KEEPER, WARDEN, JAILER -> WardenGoal.class;
            case ELEMENTAL_CASTER, BATTLE_CLERIC, NECROMANCER, HEXER, ARCANE_ARTILLERY ->
                    DungeonSpellcasterGoal.class;
            default -> null;
        };
        if (goalClass == null || mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalClass.isInstance(wrapped.getGoal()))) return;
        Goal goal = switch (role) {
            case SCOUT, AMBUSHER -> new ScoutGoal(mob);
            case BERSERKER -> new BerserkerGoal(mob);
            case SPELLBREAKER -> new SpellbreakerGoal(mob);
            case HUNTER -> new HunterGoal(mob);
            case SAPPER -> new SapperGoal(mob);
            case ELEMENTAL_CASTER, BATTLE_CLERIC, NECROMANCER, HEXER, ARCANE_ARTILLERY ->
                    new DungeonSpellcasterGoal(mob);
            default -> new WardenGoal(mob);
        };
        mob.goalSelector.addGoal(1, goal);
    }
}
