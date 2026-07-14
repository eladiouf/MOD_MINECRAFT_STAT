package tong.statmod.stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class PlayerStats {
    private static final String STATS_KEY = "stats";
    private static final String LEVEL_KEY = "level";
    private static final String XP_KEY = "xp";

    private final EnumMap<StatType, StatProgress> values = new EnumMap<>(StatType.class);

    public PlayerStats() {
        for (StatType type : StatType.values()) {
            values.put(type, new StatProgress());
        }
    }

    public StatValue get(StatType type) {
        return values.get(type).value();
    }

    public void setLevel(StatType type, int level) {
        values.get(type).setLevel(level);
    }

    public void addXp(StatType type, long amount) {
        values.get(type).addXp(amount);
    }

    public Map<StatType, StatValue> snapshot() {
        EnumMap<StatType, StatValue> copy = new EnumMap<>(StatType.class);
        values.forEach((type, progress) -> copy.put(type, progress.value()));
        return Collections.unmodifiableMap(copy);
    }

    public void copyFrom(PlayerStats source) {
        for (StatType type : StatType.values()) {
            values.get(type).copyFrom(source.values.get(type));
        }
    }

    public CompoundTag serializeNbt() {
        CompoundTag root = new CompoundTag();
        CompoundTag entries = new CompoundTag();
        for (StatType type : StatType.values()) {
            StatValue value = get(type);
            CompoundTag entry = new CompoundTag();
            entry.putInt(LEVEL_KEY, value.level());
            entry.putInt(XP_KEY, value.xp());
            entries.put(type.id(), entry);
        }
        root.put(STATS_KEY, entries);
        return root;
    }

    public void deserializeNbt(CompoundTag root) {
        if (!root.contains(STATS_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag entries = root.getCompound(STATS_KEY);
        for (StatType type : StatType.values()) {
            if (!entries.contains(type.id(), Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag entry = entries.getCompound(type.id());
            load(type, entry.getInt(LEVEL_KEY), entry.getInt(XP_KEY));
        }
    }

    void load(StatType type, int level, int xp) {
        values.get(type).load(level, xp);
    }
}
