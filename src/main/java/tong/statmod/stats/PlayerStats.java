package tong.statmod.stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class PlayerStats {
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

    void load(StatType type, int level, int xp) {
        values.get(type).load(level, xp);
    }
}
