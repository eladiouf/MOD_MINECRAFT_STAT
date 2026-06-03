package tong.statmod.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import tong.statmod.stats.StatCalculator;

public class WeaponMasteryManager implements INBTSerializable<CompoundTag> {
    public static final int WEAPON_COUNT = 13;
    private static final int MAX_LEVEL = 50;
    private final int[] levels = new int[WEAPON_COUNT];
    private final int[] xp = new int[WEAPON_COUNT];

    public int getLevel(int index) { return levels[index]; }
    public int getXp(int index) { return xp[index]; }

    public void addXp(int index, int amount) {
        if (index < 0 || index >= WEAPON_COUNT) return;
        this.xp[index] += amount;
        while (this.xp[index] >= StatCalculator.getXpForNextLevel(levels[index]) && levels[index] < MAX_LEVEL) {
            this.xp[index] -= StatCalculator.getXpForNextLevel(levels[index]);
            levels[index]++;
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("WeaponLevels", levels);
        tag.putIntArray("WeaponXP", xp);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loadedLevels = tag.getIntArray("WeaponLevels");
        int[] loadedXp = tag.getIntArray("WeaponXP");
        System.arraycopy(loadedLevels, 0, levels, 0, Math.min(loadedLevels.length, WEAPON_COUNT));
        System.arraycopy(loadedXp, 0, xp, 0, Math.min(loadedXp.length, WEAPON_COUNT));
    }
}
