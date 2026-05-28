package tong.statmod.world.thirst;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class ThirstManager implements INBTSerializable<CompoundTag> {
    private float thirst = 100;
    private static final float MAX_THIRST = 100;

    public float getThirst() { return thirst; }
    public float getThirstPercent() { return thirst / MAX_THIRST; }
    public boolean isFull() { return thirst >= MAX_THIRST; }
    public boolean isThirsty() { return thirst < MAX_THIRST; }

    public void addThirst(float amount) {
        this.thirst = Math.min(MAX_THIRST, this.thirst + amount);
    }

    public void reduceThirst(float amount) {
        this.thirst = Math.max(0, this.thirst - amount);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Thirst", thirst);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.thirst = tag.getFloat("Thirst");
    }
}
