package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tong.statmod.dungeon.ai.DungeonTacticalRole;

class IronSpellIntentPolicyTest {
    @Test void lowHealthChoosesDefense() {
        assertEquals(IronSpellIntent.DEFENSE, choose(DungeonTacticalRole.ELEMENTAL_CASTER,
                0.24f, false, 1, 10.0, true, true));
    }

    @Test void battleClericProtectsCriticalAlly() {
        assertEquals(IronSpellIntent.ALLY_SUPPORT, choose(DungeonTacticalRole.BATTLE_CLERIC,
                0.8f, true, 1, 10.0, true, true));
    }

    @Test void threatenedCasterEscapesMelee() {
        assertEquals(IronSpellIntent.MOBILITY, choose(DungeonTacticalRole.ELEMENTAL_CASTER,
                0.8f, false, 1, 2.5, true, true));
    }

    @Test void clusteredTargetsChooseAreaDamage() {
        assertEquals(IronSpellIntent.AREA_DAMAGE, choose(DungeonTacticalRole.ARCANE_ARTILLERY,
                0.8f, false, 3, 12.0, true, true));
    }

    @Test void necromancerUsesAvailableSummonBudget() {
        assertEquals(IronSpellIntent.SUMMON, choose(DungeonTacticalRole.NECROMANCER,
                0.8f, false, 1, 12.0, true, true));
    }

    @Test void hexerPrioritizesControl() {
        assertEquals(IronSpellIntent.CONTROL, choose(DungeonTacticalRole.HEXER,
                0.8f, false, 1, 12.0, false, true));
    }

    @Test void safeDefaultIsDirectDamage() {
        assertEquals(IronSpellIntent.DIRECT_DAMAGE, choose(DungeonTacticalRole.ELEMENTAL_CASTER,
                0.8f, false, 1, 12.0, false, true));
    }

    private static IronSpellIntent choose(DungeonTacticalRole role, float healthFraction,
                                          boolean criticalAlly, int nearbyEnemyCount,
                                          double targetDistance, boolean summonBudgetAvailable,
                                          boolean hasHostileTarget) {
        return IronSpellIntentPolicy.choose(new IronSpellTacticalContext(role, healthFraction,
                criticalAlly, nearbyEnemyCount, targetDistance, summonBudgetAvailable,
                hasHostileTarget));
    }
}
