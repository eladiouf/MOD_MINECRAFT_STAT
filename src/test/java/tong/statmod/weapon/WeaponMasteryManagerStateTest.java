package tong.statmod.weapon;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tong.statmod.Config;
import tong.statmod.stats.StatCalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponMasteryManagerStateTest {

    @BeforeEach
    void configureDefaults() {
        Config.xpPerLevelMultiplier = 40;
        Config.weaponMasteryMaxLevel = 50;
    }

    @Test
    void deserializeNBT_sanitizesInvalidState() {
        WeaponMasteryManager manager = new WeaponMasteryManager();
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("WeaponLevels", new int[] {999, -2});
        tag.putIntArray("WeaponXP", new int[] {9999, -8});

        manager.deserializeNBT(tag);

        assertEquals(50, manager.getLevel(0));
        assertEquals(0, manager.getXp(0));
        assertEquals(0, manager.getLevel(1));
        assertEquals(0, manager.getXp(1));
    }

    @Test
    void deserializeNBT_clearsStaleTailWhenPayloadShrinks() {
        WeaponMasteryManager manager = new WeaponMasteryManager();
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("WeaponLevels", new int[] {4});
        tag.putIntArray("WeaponXP", new int[] {5});

        manager.addXp(3, StatCalculator.getXpForNextLevel(0));
        manager.deserializeNBT(tag);

        assertEquals(4, manager.getLevel(0));
        assertEquals(5, manager.getXp(0));
        assertEquals(0, manager.getLevel(3));
        assertEquals(0, manager.getXp(3));
    }
}
