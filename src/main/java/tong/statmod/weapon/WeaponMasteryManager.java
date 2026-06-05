package tong.statmod.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import tong.statmod.stats.StatCalculator;

public class WeaponMasteryManager implements INBTSerializable<CompoundTag> {
    public static final int WEAPON_COUNT = 13;
    private final int[] levels = new int[WEAPON_COUNT];
    private final int[] xp = new int[WEAPON_COUNT];

    public int getLevel(int index) { return index >= 0 && index < WEAPON_COUNT ? levels[index] : 0; }
    public int getXp(int index) { return index >= 0 && index < WEAPON_COUNT ? xp[index] : 0; }

    public void addXp(int index, int amount) {
        if (index < 0 || index >= WEAPON_COUNT) return;
        this.xp[index] += amount;
        int maxLevel = tong.statmod.Config.weaponMasteryMaxLevel;
        while (this.xp[index] >= StatCalculator.getXpForNextLevel(levels[index]) && levels[index] < maxLevel) {
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
        java.util.Arrays.fill(levels, 0);
        java.util.Arrays.fill(xp, 0);
        int[] loadedLevels = tag.getIntArray("WeaponLevels");
        int[] loadedXp = tag.getIntArray("WeaponXP");
        System.arraycopy(loadedLevels, 0, levels, 0, Math.min(loadedLevels.length, WEAPON_COUNT));
        System.arraycopy(loadedXp, 0, xp, 0, Math.min(loadedXp.length, WEAPON_COUNT));
        sanitizeState();
    }

    private void sanitizeState() {
        int maxLevel = Math.max(0, tong.statmod.Config.weaponMasteryMaxLevel);
        for (int i = 0; i < WEAPON_COUNT; i++) {
            levels[i] = Math.max(0, Math.min(levels[i], maxLevel));
            if (levels[i] >= maxLevel) {
                xp[i] = 0;
            } else {
                int maxXp = Math.max(0, StatCalculator.getXpForNextLevel(levels[i]) - 1);
                xp[i] = Math.max(0, Math.min(xp[i], maxXp));
            }
        }
    }
}
