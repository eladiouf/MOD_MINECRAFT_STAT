package tong.statmod.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public record ClientStatsState(long revision, Map<StatType, StatValue> values) {
    public ClientStatsState {
        EnumMap<StatType, StatValue> copy = new EnumMap<>(StatType.class);
        Map<StatType, StatValue> source = values == null ? Map.of() : values;
        for (StatType type : StatType.values()) {
            copy.put(type, source.getOrDefault(type, new StatValue(0, 0)));
        }
        values = Collections.unmodifiableMap(copy);
    }
}
