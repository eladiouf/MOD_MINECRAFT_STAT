package tong.statmod.storage;

public class PlayerStatData {
    public static final int STAT_COUNT = 23;
    private final int[] levels = new int[STAT_COUNT];
    private final int[] xp = new int[STAT_COUNT];
    private final int[] perkPoints = new int[STAT_COUNT];
    private int[] unlockedPerks = new int[0];
    private int soulLevel;

    public int[] getLevels() { return levels.clone(); }
    public int[] getXp() { return xp.clone(); }
    public int[] getPerkPoints() { return perkPoints.clone(); }
    public int[] getUnlockedPerks() { return unlockedPerks.clone(); }

    public int getLevel(int index) { return index >= 0 && index < STAT_COUNT ? levels[index] : 0; }
    public int getXp(int index) { return index >= 0 && index < STAT_COUNT ? xp[index] : 0; }
    public int getPerkPointsForStat(int index) { return index >= 0 && index < STAT_COUNT ? perkPoints[index] : 0; }

    public void setLevel(int index, int value) { if (index >= 0 && index < STAT_COUNT) levels[index] = value; }
    public void setXp(int index, int value) { if (index >= 0 && index < STAT_COUNT) xp[index] = value; }
    public void setPerkPoints(int index, int value) { if (index >= 0 && index < STAT_COUNT) perkPoints[index] = value; }

    public boolean isPerkUnlocked(int perkId) {
        for (int id : unlockedPerks) if (id == perkId) return true;
        return false;
    }

    public void addUnlockedPerk(int perkId) {
        if (isPerkUnlocked(perkId)) return;
        int[] next = new int[unlockedPerks.length + 1];
        System.arraycopy(unlockedPerks, 0, next, 0, unlockedPerks.length);
        next[unlockedPerks.length] = perkId;
        unlockedPerks = next;
    }

    public void setUnlockedPerks(int[] ids) { unlockedPerks = ids.clone(); }

    public void clearUnlockedPerks() { unlockedPerks = new int[0]; }

    public int getSoulLevel() { return soulLevel; }

    public void setSoulLevel(int level) { soulLevel = Math.max(0, level); }

    public int maxStatLevel() {
        return soulLevel > 0 ? Math.min(soulLevel, 100) : 100;
    }

    public boolean addXp(int index, int amount) {
        if (index < 0 || index >= STAT_COUNT || amount <= 0) return false;
        xp[index] += amount;
        int required = requiredXp(levels[index]);
        int cap = maxStatLevel();
        boolean leveledUp = false;
        while (xp[index] >= required && levels[index] < cap) {
            levels[index]++;
            xp[index] -= required;
            required = requiredXp(levels[index]);
            leveledUp = true;
        }
        return leveledUp;
    }

    public void addLevels(int index, int amount) {
        if (index >= 0 && index < STAT_COUNT) {
            int cap = maxStatLevel();
            levels[index] = Math.min(cap, Math.max(0, levels[index] + amount));
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
