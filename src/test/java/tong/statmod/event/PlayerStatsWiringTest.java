package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

class PlayerStatsWiringTest {
    @Test
    void cloneOperationCreatesAnIndependentExactCopy() {
        PlayerStats original = new PlayerStats();
        original.setLevel(StatType.PHYSICAL_ENDURANCE, 22);
        PlayerStats clone = new PlayerStats();

        PlayerStatsEvents.copyStats(original, clone);
        original.setLevel(StatType.PHYSICAL_ENDURANCE, 2);

        assertEquals(new StatValue(22, 0), clone.get(StatType.PHYSICAL_ENDURANCE));
    }
}
