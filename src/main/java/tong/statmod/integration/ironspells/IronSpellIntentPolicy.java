package tong.statmod.integration.ironspells;

import tong.statmod.dungeon.ai.DungeonTacticalRole;

public final class IronSpellIntentPolicy {
    private static final float DEFENSE_HEALTH_THRESHOLD = 0.30f;
    private static final double MELEE_ESCAPE_DISTANCE = 4.0;

    private IronSpellIntentPolicy() {}

    public static IronSpellIntent choose(IronSpellTacticalContext context) {
        if (context.healthFraction() <= DEFENSE_HEALTH_THRESHOLD) {
            return IronSpellIntent.DEFENSE;
        }
        if (context.role() == DungeonTacticalRole.BATTLE_CLERIC && context.criticalAlly()) {
            return IronSpellIntent.ALLY_SUPPORT;
        }
        if (context.hasHostileTarget() && context.targetDistance() <= MELEE_ESCAPE_DISTANCE) {
            return IronSpellIntent.MOBILITY;
        }
        if (context.nearbyEnemyCount() >= 2) {
            return IronSpellIntent.AREA_DAMAGE;
        }
        if (context.role() == DungeonTacticalRole.NECROMANCER
                && context.summonBudgetAvailable()) {
            return IronSpellIntent.SUMMON;
        }
        if (context.role() == DungeonTacticalRole.HEXER) {
            return IronSpellIntent.CONTROL;
        }
        return IronSpellIntent.DIRECT_DAMAGE;
    }
}
