package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class DungeonTacticalRoleCoverageTest {
    @Test
    void allRolesAppearAcrossTheHundredFloorProgression() {
        EnumSet<DungeonTacticalRole> found = EnumSet.noneOf(DungeonTacticalRole.class);
        for (int floor = 1; floor <= 100; floor++) {
            for (int room = 0; room <= 8; room++) {
                for (int ordinal = 0; ordinal <= 15; ordinal++) {
                    for (DungeonFaction faction : DungeonFaction.values()) {
                        found.add(DungeonTacticalRolePolicy.roleFor(
                                faction, floor, room, ordinal, false));
                    }
                }
            }
        }
        assertEquals(EnumSet.allOf(DungeonTacticalRole.class), found);
    }
}
