package tong.statmod.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlayerStatsTest {
    @Test
    void startsWithAllStatsAtZero() {
        PlayerStats stats = new PlayerStats();

        assertEquals(23, stats.snapshot().size());
        assertEquals(new StatValue(0, 0), stats.get(StatType.BRUTE_FORCE));
    }

    @Test
    void usesQuadraticRequirementsAndHandlesMultipleLevels() {
        assertEquals(10, StatProgress.requiredXp(0));
        assertEquals(40, StatProgress.requiredXp(1));
        PlayerStats stats = new PlayerStats();

        stats.addXp(StatType.ARCANE_POWER, 55);

        assertEquals(new StatValue(2, 5), stats.get(StatType.ARCANE_POWER));
    }

    @Test
    void capsAtOneHundredAndClearsXp() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.WILLPOWER, 99);

        stats.addXp(StatType.WILLPOWER, Integer.MAX_VALUE);

        assertEquals(new StatValue(100, 0), stats.get(StatType.WILLPOWER));
    }

    @Test
    void snapshotsAndCopiesCannotMutateTheSource() {
        PlayerStats source = new PlayerStats();
        source.setLevel(StatType.COOKING, 12);
        PlayerStats copy = new PlayerStats();
        copy.copyFrom(source);

        source.setLevel(StatType.COOKING, 3);

        assertEquals(new StatValue(12, 0), copy.get(StatType.COOKING));
        assertThrows(UnsupportedOperationException.class,
                () -> copy.snapshot().put(StatType.ALCHEMY, new StatValue(1, 0)));
    }
}
