package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import tong.statmod.STATMod;

public class PlayerStats implements INBTSerializable<CompoundTag> {
    public static final int STAT_COUNT = 23;
    private final int[] levels = new int[STAT_COUNT];
    private final int[] xp = new int[STAT_COUNT];

    public int getLevel(int index) { return levels[index]; }
    public int getXp(int index) { return xp[index]; }

    public void addXp(int index, int amount) {
        if (index < 0 || index >= STAT_COUNT) return;
        this.xp[index] += amount;
        while (levels[index] < 100) {
            int required = getXpForNextLevel(levels[index]);
            if (this.xp[index] < required) break;
            this.xp[index] -= required;
            levels[index]++;
            STATMod.LOGGER.debug("Level up! Stat {} → level {}", index, levels[index]);
        }
    }

    public static int getXpForNextLevel(int level) {
        return (level + 1) * (level + 1) * 10;
    }

    public void setLevel(int index, int level) { this.levels[index] = level; }
    public void setXp(int index, int xp) { this.xp[index] = xp; }

    public void copyFrom(PlayerStats source) {
        System.arraycopy(source.levels, 0, this.levels, 0, STAT_COUNT);
        System.arraycopy(source.xp, 0, this.xp, 0, STAT_COUNT);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Levels", levels);
        tag.putIntArray("XP", xp);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loadedLevels = tag.getIntArray("Levels");
        int[] loadedXp = tag.getIntArray("XP");
        System.arraycopy(loadedLevels, 0, levels, 0, Math.min(loadedLevels.length, STAT_COUNT));
        System.arraycopy(loadedXp, 0, xp, 0, Math.min(loadedXp.length, STAT_COUNT));
    }
}
