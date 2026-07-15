package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

class AttributeEffectLevelsTest {
    @Test
    void capturesAllStatsThatDrivePersistentAttributeEffects() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.RAPIDITE, 12);
        stats.setLevel(StatType.AGILITY, 34);
        stats.setLevel(StatType.PHYSICAL_ENDURANCE, 56);

        assertEquals(new AttributeEffectLevels(12, 34, 56),
                AttributeEffectLevels.from(stats));
    }

    @Test
    void comparesSnapshotsByValue() {
        assertEquals(new AttributeEffectLevels(1, 2, 3),
                new AttributeEffectLevels(1, 2, 3));
        assertNotEquals(new AttributeEffectLevels(1, 2, 3),
                new AttributeEffectLevels(1, 3, 3));
    }
}
