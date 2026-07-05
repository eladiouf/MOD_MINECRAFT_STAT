package tong.statmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientStatCacheTest {
    @Test
    void updateAllClampsNegativeValuesForUiSafety() {
        ClientStatCache.updateAll(new int[]{-4, 7}, new int[]{-9, 12}, -3, -8, -2);

        assertEquals(0, ClientStatCache.getLevel(0));
        assertEquals(7, ClientStatCache.getLevel(1));
        assertEquals(0, ClientStatCache.getXp(0));
        assertEquals(12, ClientStatCache.getXp(1));
        assertEquals(0, ClientStatCache.getSoulLevel());
        assertEquals(0, ClientStatCache.getDungeonPoints());
        assertEquals(1, ClientStatCache.getDungeonFloorReached());
    }

    @Test
    void gettersReturnCopiesOfCachedArrays() {
        ClientStatCache.updateAll(new int[]{1}, new int[]{2}, 3, 4, 5);

        int[] levels = ClientStatCache.getLevels();
        int[] xp = ClientStatCache.getXp();
        levels[0] = 99;
        xp[0] = 99;

        assertEquals(1, ClientStatCache.getLevel(0));
        assertEquals(2, ClientStatCache.getXp(0));
    }

    @Test
    void hasLevelReflectsWhetherASyncedSnapshotExists() {
        ClientStatCache.reset();
        assertEquals(false, ClientStatCache.hasLevel(0));

        ClientStatCache.updateAll(new int[]{4}, new int[]{9}, 2, 3, 4);
        assertEquals(true, ClientStatCache.hasLevel(0));
        assertEquals(false, ClientStatCache.hasLevel(1));
    }
}
