package tong.statmod.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class ClientStatsCache {
    private static Map<StatType, StatValue> values = emptySnapshot();

    private ClientStatsCache() {
    }

    public static void replace(Map<StatType, StatValue> next) {
        values = Collections.unmodifiableMap(new EnumMap<>(next));
    }

    public static void clear() {
        values = emptySnapshot();
    }

    public static Map<StatType, StatValue> snapshot() {
        return values;
    }

    private static Map<StatType, StatValue> emptySnapshot() {
        EnumMap<StatType, StatValue> empty = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            empty.put(type, new StatValue(0, 0));
        }
        return Collections.unmodifiableMap(empty);
    }
}
