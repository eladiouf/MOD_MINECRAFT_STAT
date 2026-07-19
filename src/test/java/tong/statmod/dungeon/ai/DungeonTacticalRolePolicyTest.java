package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class DungeonTacticalRolePolicyTest {
    @Test void selectionIsDeterministic() {
        assertEquals(
                DungeonTacticalRolePolicy.roleFor(DungeonFaction.ARCANE_ORDER, 44, 3, 2, false),
                DungeonTacticalRolePolicy.roleFor(DungeonFaction.ARCANE_ORDER, 44, 3, 2, false));
    }

    @Test void earlyFloorsUseReadableRolesOnly() {
        var allowed = EnumSet.of(DungeonTacticalRole.SHIELD_CAPTAIN,
                DungeonTacticalRole.SPEAR_KEEPER, DungeonTacticalRole.SCOUT,
                DungeonTacticalRole.WARDEN);
        for (int ordinal = 0; ordinal < 30; ordinal++) {
            assertTrue(allowed.contains(DungeonTacticalRolePolicy.roleFor(
                    DungeonFaction.RESTLESS_DEAD, 5, 2, ordinal, false)));
        }
    }

    @Test void bossSquadStartsWithCommandAndMagicSupport() {
        assertEquals(DungeonTacticalRole.SHIELD_CAPTAIN,
                DungeonTacticalRolePolicy.roleFor(DungeonFaction.ARCANE_ORDER, 80, 0, 0, true));
        assertEquals(DungeonTacticalRole.ELEMENTAL_CASTER,
                DungeonTacticalRolePolicy.roleFor(DungeonFaction.ARCANE_ORDER, 80, 0, 1, true));
        assertEquals(DungeonTacticalRole.BATTLE_CLERIC,
                DungeonTacticalRolePolicy.roleFor(DungeonFaction.ARCANE_ORDER, 80, 0, 2, true));
    }
}
