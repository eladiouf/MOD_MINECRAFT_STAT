package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import java.nio.file.Files;
import java.nio.file.Path;

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
        assertTrue(StatType.ARCANE_POWER.hasPerks());
        assertTrue(StatType.ERUDITION.hasPerks());
        assertTrue(StatType.MAGIC_RESISTANCE.hasPerks());
    }

    @Test
    void testFamilies() {
        assertEquals(StatFamily.FRONTLINE_PHYSICAL_COMBAT, StatType.BRUTE_FORCE.family());
        assertEquals(StatFamily.MAGICAL_CORE, StatType.ARCANE_POWER.family());
        assertEquals(StatFamily.ELEMENTAL_SPECIALIZATION, StatType.FIRE_AFFINITY.family());
        assertEquals(StatFamily.CRAFTING_SUPPORT, StatType.ALCHEMY.family());
    }

    @Test
    void testCount() {
        assertEquals(23, StatType.values().length);
    }

    @Test
    void playerStatDataStatCountDerivesFromStatTypeEnum() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java",
                "tong", "statmod", "storage", "PlayerStatData.java"));

        assertTrue(source.contains("public static final int STAT_COUNT = StatType.values().length;"),
                "PlayerStatData.STAT_COUNT must follow StatType.values().length instead of duplicating a numeric literal");
        assertFalse(source.contains("public static final int STAT_COUNT = 23;"),
                "duplicated stat counts silently desync data arrays when stats are added or removed");
    }
}
