package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class PerkManagerTest {

    private PerkManager makeManager(int statLevel, int points) {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, statLevel);
        data.setPerkPoints(0, points);
        return new PerkManager(data);
    }

    @Test
    void testCannotUnlockWhenLevelTooLow() {
        PerkManager mgr = makeManager(5, 10);
        assertFalse(mgr.canUnlock(Perk.BRUTE_CORE));
    }

    @Test
    void testCannotUnlockWhenPointsTooLow() {
        PerkManager mgr = makeManager(10, 0);
        assertFalse(mgr.canUnlock(Perk.BRUTE_CORE));
    }

    @Test
    void testCanUnlockWithEnoughLevelAndPoints() {
        PerkManager mgr = makeManager(10, 1);
        assertTrue(mgr.canUnlock(Perk.BRUTE_CORE));
    }

    @Test
    void testUnlockDeductsPoints() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 10);
        data.setPerkPoints(0, 5);
        PerkManager mgr = new PerkManager(data);
        assertTrue(mgr.unlock(Perk.BRUTE_CORE));
        assertEquals(4, data.getPerkPointsForStat(0));
        assertTrue(data.isPerkUnlocked(0));
    }

    @Test
    void testCannotUnlockSamePerkTwice() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 10);
        data.setPerkPoints(0, 5);
        PerkManager mgr = new PerkManager(data);
        assertTrue(mgr.unlock(Perk.BRUTE_CORE));
        assertFalse(mgr.unlock(Perk.BRUTE_CORE));
    }

    @Test
    void testIsUnlocked() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 10);
        data.setPerkPoints(0, 5);
        PerkManager mgr = new PerkManager(data);
        assertFalse(mgr.isUnlocked(Perk.BRUTE_CORE));
        mgr.unlock(Perk.BRUTE_CORE);
        assertTrue(mgr.isUnlocked(Perk.BRUTE_CORE));
    }

    @Test
    void testGetPointsForStat() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(3, 7);
        PerkManager mgr = new PerkManager(data);
        assertEquals(7, mgr.getPointsForStat(3));
    }

    @Test
    void testGetUnlockedIds() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 10);
        data.setPerkPoints(0, 10);
        PerkManager mgr = new PerkManager(data);
        assertArrayEquals(new int[0], mgr.getUnlockedIds());
        mgr.unlock(Perk.BRUTE_CORE);
        assertArrayEquals(new int[]{0}, mgr.getUnlockedIds());
    }

    @Test
    void testResetAll() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 10);
        data.setPerkPoints(0, 5);
        PerkManager mgr = new PerkManager(data);
        mgr.unlock(Perk.BRUTE_CORE);
        assertTrue(data.isPerkUnlocked(0));
        mgr.resetAll();
        assertFalse(data.isPerkUnlocked(0));
    }

    @Test
    void testActiveTierUnlock() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 25);
        data.setPerkPoints(0, 5);
        PerkManager mgr = new PerkManager(data);
        assertTrue(mgr.canUnlock(Perk.BRUTE_ACTIVE));
        assertTrue(mgr.unlock(Perk.BRUTE_ACTIVE));
        assertEquals(4, data.getPerkPointsForStat(0));
    }

    @Test
    void testCannotUnlockNullPerk() {
        PerkManager mgr = makeManager(10, 5);
        assertFalse(mgr.canUnlock(null));
        assertFalse(mgr.unlock(null));
    }

    @Test
    void testGrantDoesNotDeductPoints() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(0, 5);
        PerkManager mgr = new PerkManager(data);

        assertTrue(mgr.grant(Perk.BRUTE_TRANSCENDENCE));
        assertTrue(data.isPerkUnlocked(Perk.BRUTE_TRANSCENDENCE.id));
        assertEquals(5, data.getPerkPointsForStat(0));
    }

    @Test
    void testRevokeRefundsPoints() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);
        PerkManager mgr = new PerkManager(data);

        assertTrue(mgr.revoke(Perk.BRUTE_CORE, true));
        assertFalse(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
        assertEquals(1, data.getPerkPointsForStat(0));
    }
}
