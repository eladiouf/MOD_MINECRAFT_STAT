package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Records personnels (2026-07-09) — verrouille les règles pures : seuil de célébration du combo,
 * comparaison des temps (0 = pas de record), format d'affichage.
 */
public class DungeonRecordsTest {

    @Test
    void comboRecordNeedsMinimumAndImprovement() {
        assertFalse(DungeonRecords.isComboRecord(4, 0), "sous le seuil de 5, jamais de record");
        assertTrue(DungeonRecords.isComboRecord(5, 0), "premier record dès 5");
        assertTrue(DungeonRecords.isComboRecord(12, 11));
        assertFalse(DungeonRecords.isComboRecord(11, 11), "égaler n'est pas battre");
        assertFalse(DungeonRecords.isComboRecord(8, 20));
    }

    @Test
    void clearRecordComparesTimes() {
        assertTrue(DungeonRecords.isClearRecord(1200, 0), "premier temps = record");
        assertTrue(DungeonRecords.isClearRecord(900, 1200), "plus rapide = record");
        assertFalse(DungeonRecords.isClearRecord(1200, 900));
        assertFalse(DungeonRecords.isClearRecord(1200, 1200), "égaler n'est pas battre");
        assertFalse(DungeonRecords.isClearRecord(0, 900), "durée nulle = invalide");
    }

    @Test
    void ticksFormatAsMinutesSecondsTenths() {
        assertEquals("0:00.5", DungeonRecords.formatTicks(10));     // 10 ticks = 0,5 s
        assertEquals("0:01.0", DungeonRecords.formatTicks(20));
        assertEquals("1:00.0", DungeonRecords.formatTicks(1200));   // 60 s
        assertEquals("2:30.5", DungeonRecords.formatTicks(3010));   // 150,5 s
    }
}
