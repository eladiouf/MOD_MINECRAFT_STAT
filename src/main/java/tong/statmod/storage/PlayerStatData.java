package tong.statmod.storage;

public class PlayerStatData {
    public static final int STAT_COUNT = 23;
    private final int[] levels = new int[STAT_COUNT];
    private final int[] xp = new int[STAT_COUNT];
    private final int[] perkPoints = new int[STAT_COUNT];

    public int[] getLevels() { return levels.clone(); }
    public int[] getXp() { return xp.clone(); }
    public int[] getPerkPoints() { return perkPoints.clone(); }

    public int getLevel(int index) { return index >= 0 && index < STAT_COUNT ? levels[index] : 0; }
    public int getXp(int index) { return index >= 0 && index < STAT_COUNT ? xp[index] : 0; }
    public int getPerkPointsForStat(int index) { return index >= 0 && index < STAT_COUNT ? perkPoints[index] : 0; }

    public void setLevel(int index, int value) { if (index >= 0 && index < STAT_COUNT) levels[index] = value; }
    public void setXp(int index, int value) { if (index >= 0 && index < STAT_COUNT) xp[index] = value; }
    public void setPerkPoints(int index, int value) { if (index >= 0 && index < STAT_COUNT) perkPoints[index] = value; }

    public boolean addXp(int index, int amount) {
        if (index < 0 || index >= STAT_COUNT || amount <= 0) return false;
        xp[index] += amount;
        int required = requiredXp(levels[index]);
        boolean leveledUp = false;
        while (xp[index] >= required && levels[index] < 100) {
            levels[index]++;
            xp[index] -= required;
            required = requiredXp(levels[index]);
            leveledUp = true;
        }
        return leveledUp;
    }

    public void addLevels(int index, int amount) {
        if (index >= 0 && index < STAT_COUNT) {
            levels[index] = Math.min(100, Math.max(0, levels[index] + amount));
        }
    }

    public void addPerkPointsForStat(int index, int amount) {
        if (index >= 0 && index < STAT_COUNT) {
            perkPoints[index] = Math.max(0, perkPoints[index] + amount);
        }
    }

    public int getGlobalLevel() {
        int sum = 0;
        for (int l : levels) sum += l;
        return sum / STAT_COUNT;
    }

    public static int requiredXp(int level) {
        return (level + 1) * (level + 1) * 10;
    }
}
