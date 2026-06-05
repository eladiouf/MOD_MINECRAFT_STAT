package tong.statmod.fatigue;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class FatigueManager implements INBTSerializable<CompoundTag> {
    private float fatigue = 0;
    private int maxFatigue = 200;
    private int sleeplessNights = 0;
    private long lastSleepTime = 0;

    public float getFatigue() { return fatigue; }
    public int getMaxFatigue() { return maxFatigue; }
    public void setMaxFatigue(int max) { this.maxFatigue = Math.max(50, max); }
    public float getFatiguePercent() { return fatigue / (float)maxFatigue; }
    public boolean isExhausted() { return fatigue >= maxFatigue; }

    public int getSleeplessNights() { return sleeplessNights; }
    public void setSleeplessNights(int n) { this.sleeplessNights = n; }
    public void incrementSleeplessNights() { this.sleeplessNights++; }
    public long getLastSleepTime() { return lastSleepTime; }
    public void setLastSleepTime(long time) { this.lastSleepTime = time; }

    public void addFatigue(float amount) {
        this.fatigue = Math.min(maxFatigue, this.fatigue + amount);
    }

    public void reduceFatigue(float amount) {
        this.fatigue = Math.max(0, this.fatigue - amount);
    }

    public void reset() {
        this.fatigue = 0;
        this.sleeplessNights = 0;
    }

    public void markSlept(long gameTime) {
        this.fatigue = 0;
        this.sleeplessNights = 0;
        this.lastSleepTime = gameTime;
    }

    public FatigueThreshold getThreshold() {
        float pct = getFatiguePercent();
        if (pct >= 1.0f) return FatigueThreshold.EXHAUSTED;
        if (pct >= 0.9f) return FatigueThreshold.CRITICAL;
        if (pct >= 0.75f) return FatigueThreshold.SEVERE;
        if (pct >= 0.5f) return FatigueThreshold.MODERATE;
        if (pct >= 0.25f) return FatigueThreshold.LIGHT;
        if (pct >= 0.1f) return FatigueThreshold.WARNING;
        return FatigueThreshold.NONE;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Fatigue", fatigue);
        tag.putInt("SleeplessNights", sleeplessNights);
        tag.putLong("LastSleepTime", lastSleepTime);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.fatigue = tag.getFloat("Fatigue");
        this.sleeplessNights = tag.getInt("SleeplessNights");
        this.lastSleepTime = tag.getLong("LastSleepTime");
        sanitizeState();
    }

    private void sanitizeState() {
        this.maxFatigue = Math.max(50, this.maxFatigue);
        this.fatigue = Math.max(0, Math.min(this.fatigue, this.maxFatigue));
        this.sleeplessNights = Math.max(0, this.sleeplessNights);
        this.lastSleepTime = Math.max(0, this.lastSleepTime);
    }

    public enum FatigueThreshold {
        NONE, WARNING, LIGHT, MODERATE, SEVERE, CRITICAL, EXHAUSTED
    }
}
