package tong.statmod.client;

import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class ClientStatsCache {
    private static volatile ClientStatsState state = zeroState(0);

    private ClientStatsCache() {
    }

    public static void replace(Map<StatType, StatValue> next) {
        replace(next, List.of());
    }

    public static void replace(Map<StatType, StatValue> next, List<String> activePerkIds) {
        state = new ClientStatsState(state.revision() + 1, next, activePerkIds);
    }

    public static void clear() {
        state = zeroState(state.revision() + 1);
    }

    public static ClientStatsState state() {
        return state;
    }

    public static Map<StatType, StatValue> snapshot() {
        return state.values();
    }

    private static ClientStatsState zeroState(long revision) {
        EnumMap<StatType, StatValue> empty = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            empty.put(type, new StatValue(0, 0));
        }
        return new ClientStatsState(revision, empty, List.of());
    }
}
