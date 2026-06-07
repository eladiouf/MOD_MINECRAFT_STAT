package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class MobStats implements INBTSerializable<CompoundTag> {
    public static final int STAT_COUNT = 23;

    private final int[] statLevels = new int[STAT_COUNT];

    public int getLevel(int index) {
        return statLevels[index];
    }

    public void setLevel(int index, int level) {
        if (index < 0 || index >= STAT_COUNT) return;
        statLevels[index] = Math.max(0, Math.min(100, level));
    }

    public void addToLevel(int index, int amount) {
        setLevel(index, getLevel(index) + amount);
    }

    public void reset() {
        java.util.Arrays.fill(statLevels, 0);
    }

    public void sanitize() {
        for (int i = 0; i < STAT_COUNT; i++) {
            statLevels[i] = Math.max(0, Math.min(100, statLevels[i]));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("MobStatLevels", statLevels.clone());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loaded = tag.getIntArray("MobStatLevels");
        if (loaded.length >= STAT_COUNT) {
            System.arraycopy(loaded, 0, statLevels, 0, STAT_COUNT);
        }
        sanitize();
    }
}
