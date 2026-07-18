package tong.statmod.integration.ironspells;

import tong.statmod.dungeon.ai.DungeonTacticalRole;

public record IronSpellTacticalContext(
        DungeonTacticalRole role,
        float healthFraction,
        boolean criticalAlly,
        int nearbyEnemyCount,
        double targetDistance,
        boolean summonBudgetAvailable,
        boolean hasHostileTarget) {
}
