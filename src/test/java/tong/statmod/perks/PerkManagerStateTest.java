package tong.statmod.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PerkManagerStateTest {

    @Test
    void unlockedPerks_viewIsImmutable() {
        PerkManager manager = new PerkManager();
        manager.addPoints(1);
        manager.unlockPerk(Perk.BRUTE_DEMOLITION, 100);

        assertThrows(UnsupportedOperationException.class, () -> manager.getUnlockedPerks().clear());
    }

    @Test
    void deserializeNBT_filtersInvalidPerksAndNegativePoints() {
        PerkManager manager = new PerkManager();
        CompoundTag tag = new CompoundTag();
        tag.putInt("Points", -4);
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(Perk.BRUTE_DEMOLITION.id));
        list.add(IntTag.valueOf(999));
        tag.put("UnlockedPerks", list);

        manager.deserializeNBT(tag);

        assertEquals(0, manager.getAvailablePoints());
        assertTrue(manager.isUnlocked(Perk.BRUTE_DEMOLITION));
        assertFalse(manager.isUnlocked(999));
    }
}
