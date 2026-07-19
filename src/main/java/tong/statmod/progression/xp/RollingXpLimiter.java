package tong.statmod.progression.xp;

import java.util.ArrayDeque;
import java.util.EnumMap;
import tong.statmod.stats.StatType;

final class RollingXpLimiter {
    static final long WINDOW_TICKS = 1_200L;

    private final EnumMap<StatType, ArrayDeque<Entry>> entries =
            new EnumMap<>(StatType.class);

    int remaining(StatType stat, long tick, int cap) {
        ArrayDeque<Entry> window = entries.get(stat);
        if (window == null) {
            return Math.max(0, cap);
        }
        while (!window.isEmpty() && tick - window.peekFirst().tick() >= WINDOW_TICKS) {
            window.removeFirst();
        }
        int used = window.stream().mapToInt(Entry::amount).sum();
        return Math.max(0, cap - used);
    }

    void record(StatType stat, int amount, long tick) {
        if (amount > 0) {
            entries.computeIfAbsent(stat, ignored -> new ArrayDeque<>())
                    .addLast(new Entry(tick, amount));
        }
    }

    private record Entry(long tick, int amount) {
    }
}
