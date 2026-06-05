package tong.statmod.world.thirst;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThirstManagerStateTest {

    @Test
    void deserializeNBT_sanitizesInvalidValues() {
        ThirstManager manager = new ThirstManager();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Thirst", 999f);

        manager.deserializeNBT(tag);

        assertEquals(100f, manager.getThirst());
    }

    @Test
    void deserializeNBT_clampsNegativeValuesToZero() {
        ThirstManager manager = new ThirstManager();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Thirst", -5f);

        manager.deserializeNBT(tag);

        assertEquals(0f, manager.getThirst());
    }
}
