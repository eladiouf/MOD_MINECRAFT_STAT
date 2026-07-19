package tong.statmod.stats;

public final class StatProgress {
    public static final int MAX_LEVEL = 100;

    private int level;
    private int xp;

    public static int requiredXp(int level) {
        if (level < 0 || level >= MAX_LEVEL) {
            return 0;
        }
        return 10 * (level + 1) * (level + 1);
    }

    StatValue value() {
        return new StatValue(level, xp);
    }

    void setLevel(int requestedLevel) {
        level = Math.max(0, Math.min(MAX_LEVEL, requestedLevel));
        xp = 0;
    }

    void load(int requestedLevel, int requestedXp) {
        level = Math.max(0, Math.min(MAX_LEVEL, requestedLevel));
        xp = 0;
        if (level < MAX_LEVEL) {
            addXp(Math.max(0, requestedXp));
        }
    }

    void addXp(long amount) {
        if (amount <= 0 || level == MAX_LEVEL) {
            return;
        }

        long remaining = (long) xp + amount;
        while (level < MAX_LEVEL) {
            int required = requiredXp(level);
            if (remaining < required) {
                break;
            }
            remaining -= required;
            level++;
        }
        xp = level == MAX_LEVEL ? 0 : (int) remaining;
    }

    void copyFrom(StatProgress source) {
        level = source.level;
        xp = source.xp;
    }
}
