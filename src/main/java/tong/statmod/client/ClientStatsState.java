package tong.statmod.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public record ClientStatsState(
        long revision, Map<StatType, StatValue> values, List<String> activePerkIds,
        int dungeonPoints, int dungeonFloorReached) {
    public ClientStatsState {
        EnumMap<StatType, StatValue> copy = new EnumMap<>(StatType.class);
        Map<StatType, StatValue> source = values == null ? Map.of() : values;
        for (StatType type : StatType.values()) {
            copy.put(type, source.getOrDefault(type, new StatValue(0, 0)));
        }
        values = Collections.unmodifiableMap(copy);
        activePerkIds = List.copyOf(activePerkIds == null ? List.of() : activePerkIds);
    }

    public ClientStatsState(long revision, Map<StatType, StatValue> values) {
        this(revision, values, List.of(), 0, 1);
    }

    public ClientStatsState(long revision, Map<StatType, StatValue> values, List<String> activePerkIds) {
        this(revision, values, activePerkIds, 0, 1);
    }
}
