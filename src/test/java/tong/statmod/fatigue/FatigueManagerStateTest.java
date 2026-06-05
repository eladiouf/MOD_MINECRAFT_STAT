package tong.statmod.fatigue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FatigueManagerStateTest {

    @Test
    void deserializeNBT_sanitizesInvalidValues() {
        FatigueManager manager = new FatigueManager();
        manager.setMaxFatigue(200);
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Fatigue", 999f);
        tag.putInt("SleeplessNights", -4);
        tag.putLong("LastSleepTime", -80L);

        manager.deserializeNBT(tag);

        assertEquals(200f, manager.getFatigue());
        assertEquals(0, manager.getSleeplessNights());
        assertEquals(0L, manager.getLastSleepTime());
    }

    @Test
    void deserializeNBT_clampsNegativeFatigueToZero() {
        FatigueManager manager = new FatigueManager();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Fatigue", -12f);

        manager.deserializeNBT(tag);

        assertEquals(0f, manager.getFatigue());
    }
}
