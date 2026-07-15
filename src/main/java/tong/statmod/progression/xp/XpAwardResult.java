package tong.statmod.progression.xp;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import tong.statmod.stats.StatType;

public record XpAwardResult(boolean changed, Map<StatType, Integer> accepted) {
    public XpAwardResult {
        EnumMap<StatType, Integer> copy = new EnumMap<>(StatType.class);
        if (accepted != null) {
            copy.putAll(accepted);
        }
        accepted = Collections.unmodifiableMap(copy);
    }
}
