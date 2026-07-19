package tong.statmod.progression.xp;

import java.util.List;

public final class EnchantmentStudyXp {
    public static final int MAX_PER_BOOK = 160;

    private EnchantmentStudyXp() {
    }

    public static int calculate(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        long total = 0L;
        for (Entry entry : entries) {
            if (entry == null || entry.level() <= 0 || entry.rarityWeight() <= 0) {
                continue;
            }
            long units = Math.min(MAX_PER_BOOK,
                    (long) entry.level() * entry.rarityWeight());
            total = Math.min(MAX_PER_BOOK, total + units * 5L);
        }
        return (int) total;
    }

    public record Entry(int level, int rarityWeight) {
    }
}
