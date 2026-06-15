package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import static org.junit.jupiter.api.Assertions.*;

class StatTypeTest {

    @Test
    void testAllStatsHaveIndices() {
        for (StatType stat : StatType.values()) {
            assertNotNull(stat.displayName);
            assertNotNull(stat.description);
            assertTrue(stat.index >= 0);
        }
    }

    @Test
    void testByIndex() {
        assertEquals(StatType.BRUTE_FORCE, StatType.byIndex(0));
        assertEquals(StatType.WILLPOWER, StatType.byIndex(22));
        assertNull(StatType.byIndex(-1));
        assertNull(StatType.byIndex(23));
    }

    @Test
    void testHasPerks() {
        assertTrue(StatType.BRUTE_FORCE.hasPerks());
        assertTrue(StatType.WILLPOWER.hasPerks());
        assertFalse(StatType.ARCANE_POWER.hasPerks());
        assertFalse(StatType.ERUDITION.hasPerks());
    }

    @Test
    void testCount() {
        assertEquals(23, StatType.values().length);
    }
}
