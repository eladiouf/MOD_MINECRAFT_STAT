package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobStatsTest {

    @Test
    void defaultLevelsAreZero() {
        MobStats stats = new MobStats();
        for (int i = 0; i < MobStats.STAT_COUNT; i++) {
            assertEquals(0, stats.getLevel(i));
        }
    }

    @Test
    void setLevel_clampsAt100() {
        MobStats stats = new MobStats();
        stats.setLevel(0, 150);
        assertEquals(100, stats.getLevel(0));
    }

    @Test
    void setLevel_clampsAtZero() {
        MobStats stats = new MobStats();
        stats.setLevel(0, -5);
        assertEquals(0, stats.getLevel(0));
    }

    @Test
    void nbtRoundTrip_preservesAllLevels() {
        MobStats original = new MobStats();
        for (int i = 0; i < MobStats.STAT_COUNT; i++) {
            original.setLevel(i, i * 4);
        }
        CompoundTag tag = original.serializeNBT();
        MobStats loaded = new MobStats();
        loaded.deserializeNBT(tag);
        for (int i = 0; i < MobStats.STAT_COUNT; i++) {
            assertEquals(original.getLevel(i), loaded.getLevel(i),
                "Stat index " + i + " must survive NBT round-trip");
        }
    }

    @Test
    void deserializeNBT_sanitizesCorruptedValues() {
        CompoundTag tag = new CompoundTag();
        int[] badValues = new int[MobStats.STAT_COUNT];
        badValues[0] = 999;
        badValues[1] = -10;
        tag.putIntArray("MobStatLevels", badValues);

        MobStats stats = new MobStats();
        stats.deserializeNBT(tag);

        assertEquals(100, stats.getLevel(0), "999 must be clamped to 100");
        assertEquals(0,   stats.getLevel(1), "-10 must be clamped to 0");
    }
}
