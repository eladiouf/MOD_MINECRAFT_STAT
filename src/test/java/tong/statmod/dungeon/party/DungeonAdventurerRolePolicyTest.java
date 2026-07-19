package tong.statmod.dungeon.party;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DungeonAdventurerRolePolicyTest {
    @Test
    void selectionIsDeterministic() {
        assertEquals(
                DungeonAdventurerRolePolicy.roleFor(42, 3, 1, false),
                DungeonAdventurerRolePolicy.roleFor(42, 3, 1, false));
    }

    @Test
    void bossSquadStartsWithTankThenUsesMagicSupport() {
        assertEquals(PartyRole.TANK, DungeonAdventurerRolePolicy.roleFor(60, 0, 0, true));
        assertEquals(PartyRole.MAGE, DungeonAdventurerRolePolicy.roleFor(60, 0, 1, true));
        assertEquals(PartyRole.HEALER, DungeonAdventurerRolePolicy.roleFor(60, 0, 2, true));
    }
}
