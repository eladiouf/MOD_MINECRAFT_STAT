package tong.statmod.progression;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionRoutingTest {

    @Test
    void combatWeaponRoutingUsesOnePrimaryStat() {
        assertEquals(StatType.BLADE_TECHNIQUE, ProgressionRouting.combatStatForWeaponCategory("SWORD"));
        assertEquals(StatType.BRUTE_FORCE, ProgressionRouting.combatStatForWeaponCategory("AXE"));
        assertEquals(StatType.PRECISION, ProgressionRouting.combatStatForWeaponCategory("BOW"));
        assertEquals(StatType.PRECISION, ProgressionRouting.combatStatForWeaponCategory("TRIDENT"));
    }

    @Test
    void nonCombatRoutingMapsEachActionToOneStat() {
        assertEquals(StatType.FORGING, ProgressionRouting.nonCombatStatFor(ActionType.MINING));
        assertEquals(StatType.FORGING, ProgressionRouting.nonCombatStatFor(ActionType.CRAFTING));
        assertEquals(StatType.ALCHEMY, ProgressionRouting.nonCombatStatFor(ActionType.BREWING));
        assertEquals(StatType.COOKING, ProgressionRouting.nonCombatStatFor(ActionType.COOKING));
    }

    @Test
    void actionTypesExposeTheirPrimaryStat() {
        assertEquals(StatType.FORGING, ActionType.MINING.primaryStat());
        assertEquals(StatType.FORGING, ActionType.CRAFTING.primaryStat());
        assertEquals(StatType.ALCHEMY, ActionType.BREWING.primaryStat());
        assertEquals(StatType.COOKING, ActionType.COOKING.primaryStat());
    }
}
