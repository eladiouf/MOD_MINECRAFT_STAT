package tong.statmod.fatigue;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class FatigueManager implements INBTSerializable<CompoundTag> {
    private float fatigue = 0;
    private static final float MAX_FATIGUE = 100;

    public float getFatigue() { return fatigue; }
    public float getFatiguePercent() { return fatigue / MAX_FATIGUE; }
    public boolean isExhausted() { return fatigue >= MAX_FATIGUE; }

    public void addFatigue(float amount) {
        this.fatigue = Math.min(MAX_FATIGUE, this.fatigue + amount);
    }

    public void reduceFatigue(float amount) {
        this.fatigue = Math.max(0, this.fatigue - amount);
    }

    public void reset() { this.fatigue = 0; }

    public FatigueThreshold getThreshold() {
        float pct = getFatiguePercent();
        if (pct >= 1.0f) return FatigueThreshold.EXHAUSTED;
        if (pct >= 0.9f) return FatigueThreshold.CRITICAL;
        if (pct >= 0.75f) return FatigueThreshold.SEVERE;
        if (pct >= 0.5f) return FatigueThreshold.MODERATE;
        if (pct >= 0.25f) return FatigueThreshold.LIGHT;
        return FatigueThreshold.NONE;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Fatigue", fatigue);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.fatigue = tag.getFloat("Fatigue");
    }

    public enum FatigueThreshold {
        NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EXHAUSTED
    }
}
