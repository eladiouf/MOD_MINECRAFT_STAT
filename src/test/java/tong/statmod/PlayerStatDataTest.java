package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatDataTest {

    @Test
    void testInitialValues() {
        PlayerStatData data = new PlayerStatData();
        for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
            assertEquals(0, data.getLevel(i));
            assertEquals(0, data.getXp(i));
            assertEquals(0, data.getPerkPointsForStat(i));
        }
        assertEquals(0, data.getGlobalLevel());
        assertArrayEquals(new int[0], data.getUnlockedPerks());
    }

    @Test
    void testAddXp() {
        PlayerStatData data = new PlayerStatData();
        assertFalse(data.addXp(0, 0));
        assertFalse(data.addXp(-1, 10));
        assertFalse(data.addXp(PlayerStatData.STAT_COUNT, 10));
        assertTrue(data.addXp(0, 10));
        assertEquals(0, data.getXp(0));
        assertEquals(1, data.getLevel(0));
    }

    @Test
    void testAddXpNoLevelUp() {
        PlayerStatData data = new PlayerStatData();
        data.addXp(0, 5);
        assertEquals(5, data.getXp(0));
        assertEquals(0, data.getLevel(0));
    }

    @Test
    void testAddXpMultipleLevels() {
        PlayerStatData data = new PlayerStatData();
        data.addXp(0, 10000);
        assertTrue(data.getLevel(0) > 10);
        assertTrue(data.getXp(0) >= 0);
    }

    @Test
    void testLevelCap() {
        PlayerStatData data = new PlayerStatData();
        for (int i = 0; i < 200; i++) data.addXp(0, 999999);
        assertEquals(100, data.getLevel(0));
    }

    @Test
    void testAddLevels() {
        PlayerStatData data = new PlayerStatData();
        data.addLevels(0, 5);
        assertEquals(5, data.getLevel(0));
        data.addLevels(0, -2);
        assertEquals(3, data.getLevel(0));
        data.addLevels(0, 200);
        assertEquals(100, data.getLevel(0));
    }

    @Test
    void testPerkPoints() {
        PlayerStatData data = new PlayerStatData();
        data.addPerkPointsForStat(0, 5);
        assertEquals(5, data.getPerkPointsForStat(0));
        data.addPerkPointsForStat(0, -2);
        assertEquals(3, data.getPerkPointsForStat(0));
        data.addPerkPointsForStat(0, -10);
        assertEquals(0, data.getPerkPointsForStat(0));
    }

    @Test
    void testPerkPointsAreSharedAcrossStatsInTheSameFamily() {
        PlayerStatData data = new PlayerStatData();

        data.setPerkPoints(StatType.BRUTE_FORCE.index, 4);

        assertEquals(4, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(4, data.getPerkPointsForStat(StatType.BLADE_TECHNIQUE.index));

        data.addPerkPointsForStat(StatType.BLADE_TECHNIQUE.index, -1);

        assertEquals(3, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(3, data.getPerkPointsForStat(StatType.BLADE_TECHNIQUE.index));
    }

    @Test
    void testGlobalLevel() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(0, data.getGlobalLevel());
        data.setLevel(0, 46);
        assertEquals(2, data.getGlobalLevel());
    }

    @Test
    void testRequiredXp() {
        assertEquals(10, PlayerStatData.requiredXp(0));
        assertEquals(40, PlayerStatData.requiredXp(1));
        assertEquals(90, PlayerStatData.requiredXp(2));
        assertEquals(100000, PlayerStatData.requiredXp(99));
    }

    @Test
    void testUnlockedPerks() {
        PlayerStatData data = new PlayerStatData();
        assertFalse(data.isPerkUnlocked(0));
        assertFalse(data.isPerkUnlocked(83));

        data.addUnlockedPerk(0);
        assertTrue(data.isPerkUnlocked(0));
        assertFalse(data.isPerkUnlocked(1));

        data.addUnlockedPerk(83);
        assertTrue(data.isPerkUnlocked(83));

        data.addUnlockedPerk(0);
        assertTrue(data.isPerkUnlocked(0));
        assertEquals(2, data.getUnlockedPerks().length);
    }

    @Test
    void testClearUnlockedPerks() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(0);
        data.addUnlockedPerk(1);
        data.clearUnlockedPerks();
        assertArrayEquals(new int[0], data.getUnlockedPerks());
    }

    @Test
    void testSetUnlockedPerks() {
        PlayerStatData data = new PlayerStatData();
        data.setUnlockedPerks(new int[]{10, 20, 30});
        assertTrue(data.isPerkUnlocked(10));
        assertTrue(data.isPerkUnlocked(20));
        assertTrue(data.isPerkUnlocked(30));
        assertFalse(data.isPerkUnlocked(11));
    }
}
