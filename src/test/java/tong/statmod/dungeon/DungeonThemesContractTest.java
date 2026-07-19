package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DungeonThemesContractTest {
    @Test
    void everyBaseFloorHasUsableCataclysmFreePools() {
        for (int floor = 1; floor <= 100; floor++) {
            DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
            assertNotNull(theme, "floor " + floor);
            assertFalse(theme.adds().isEmpty(), "adds floor " + floor);
            assertFalse(theme.miniBoss().isEmpty(), "mini boss floor " + floor);
            theme.adds().forEach(id -> assertFalse(id.startsWith("cataclysm:"), id));
            theme.miniBoss().forEach(id -> assertFalse(id.startsWith("cataclysm:"), id));
        }
    }
}
