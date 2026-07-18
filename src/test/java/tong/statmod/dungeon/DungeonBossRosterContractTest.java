package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DungeonBossRosterContractTest {
    @Test
    void firstThousandFloorsNeverSelectCataclysm() {
        for (int floor = 10; floor <= 1000; floor += 10) {
            var roster = DungeonBossRoster.forFloor(floor);
            assertFalse(roster.isEmpty(), "floor " + floor);
            for (var entry : roster) {
                assertFalse(entry.entityId().contains("cataclysm:"), entry.entityId());
                assertTrue(entry.entityId().split(",").length <= 3, entry.entityId());
            }
        }
    }
}
