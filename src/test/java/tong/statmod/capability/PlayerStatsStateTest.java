package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tong.statmod.Config;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStatsStateTest {

    @BeforeEach
    void configureDefaults() {
        Config.xpPerLevelMultiplier = 40;
    }

    @Test
    void deserializeNBT_sanitizesInvalidState() {
        PlayerStats stats = new PlayerStats();
        CompoundTag tag = new CompoundTag();
        int[] levels = new int[PlayerStats.STAT_COUNT];
        int[] xp = new int[PlayerStats.STAT_COUNT];
        levels[0] = 150;
        xp[0] = 9999;
        levels[1] = -3;
        xp[1] = -9;
        levels[StatType.MANA_POOL.index] = 100;
        xp[StatType.MANA_POOL.index] = 9999;
        tag.putIntArray("Levels", levels);
        tag.putIntArray("XP", xp);
        tag.putFloat("Mana", 999f);
        tag.putLong("ManaBlockTick", -50L);

        stats.deserializeNBT(tag);

        assertEquals(100, stats.getLevel(0));
        assertEquals(0, stats.getXp(0));
        assertEquals(0, stats.getLevel(1));
        assertEquals(0, stats.getXp(1));
        assertEquals(stats.getMaxMana(), stats.getMana());
        assertEquals(0L, stats.getManaBlockRemainingTicks());
    }

    @Test
    void deserializeNBT_clearsStaleTailWhenPayloadShrinks() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(5, 42);
        stats.setXp(5, 17);

        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Levels", new int[] {3});
        tag.putIntArray("XP", new int[] {4});

        stats.deserializeNBT(tag);

        assertEquals(3, stats.getLevel(0));
        assertEquals(4, stats.getXp(0));
        assertEquals(0, stats.getLevel(5));
        assertEquals(0, stats.getXp(5));
    }
}
