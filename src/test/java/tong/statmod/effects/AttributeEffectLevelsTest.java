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
        stats.setLevel(StatType.ARCANE_POWER, 11);
        stats.setLevel(StatType.CASTING_SPEED, 22);
        stats.setLevel(StatType.MANA_POOL, 33);
        stats.setLevel(StatType.MAGIC_RESISTANCE, 44);

        assertEquals(new AttributeEffectLevels(12, 34, 56, 11, 22, 33, 44),
                AttributeEffectLevels.from(stats));
    }

    @Test
    void comparesSnapshotsByValue() {
        assertEquals(new AttributeEffectLevels(1, 2, 3, 4, 5, 6, 7),
                new AttributeEffectLevels(1, 2, 3, 4, 5, 6, 7));
        assertNotEquals(new AttributeEffectLevels(1, 2, 3, 4, 5, 6, 7),
                new AttributeEffectLevels(1, 2, 3, 4, 5, 6, 8));
    }
}
