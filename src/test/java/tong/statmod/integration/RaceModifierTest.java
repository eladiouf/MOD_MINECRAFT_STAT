package tong.statmod.integration;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RaceModifierTest {

    @Test
    void rejectsUnknownStatIndexes() {
        assertThrows(IllegalArgumentException.class,
                () -> new RaceModifier(-1, 1, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new RaceModifier(PlayerStatData.STAT_COUNT, 1, 1.0));
    }

    @Test
    void keepsValidStatIndexesAndClampsXpMultiplier() {
        RaceModifier low = new RaceModifier(StatType.BRUTE_FORCE.index, 2, 0.0);
        RaceModifier high = new RaceModifier(StatType.BRUTE_FORCE.index, 2, 10.0);

        assertEquals(StatType.BRUTE_FORCE.index, low.statIndex());
        assertEquals(0.1, low.xpMultiplier());
        assertEquals(5.0, high.xpMultiplier());
    }
}
