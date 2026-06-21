package tong.statmod.storage;

import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

public class PlayerStatData {
    public static final int STAT_COUNT = 23;
    public static final int PERK_FAMILY_COUNT = StatFamily.values().length;
    private final int[] levels = new int[STAT_COUNT];
    private final int[] xp = new int[STAT_COUNT];
    private final int[] perkPoints = new int[PERK_FAMILY_COUNT];
    private int[] unlockedPerks = new int[0];
    private int[] freeGrantedPerks = new int[0];
    private int soulLevel;

    // Magic tree state
    private int arcanePoints;
    private final int[] schoolPoints = new int[tong.statmod.magic.MagicBranch.values().length];
    private final int[] schoolMasteryProgress = new int[tong.statmod.magic.MagicBranch.values().length];
    private String[] magicNodes = new String[0];
    private String[] learnedSpells = new String[0];
    private tong.statmod.magic.MagicRace magicRace;
    private tong.statmod.magic.MagicBranch chosenStartBranch;

    public int[] getLevels() { return levels.clone(); }
    public int[] getXp() { return xp.clone(); }
    public int[] getPerkPoints() { return perkPoints.clone(); }
    public int[] getUnlockedPerks() { return unlockedPerks.clone(); }
    public int[] getFreeGrantedPerks() { return freeGrantedPerks.clone(); }

    public int getLevel(int index) { return index >= 0 && index < STAT_COUNT ? levels[index] : 0; }
    public int getXp(int index) { return index >= 0 && index < STAT_COUNT ? xp[index] : 0; }
    public int getPerkPointsForStat(int index) {
        StatType stat = StatType.byIndex(index);
        return stat != null ? getPerkPointsForFamily(stat.family()) : 0;
    }

    public int getPerkPointsForFamily(StatFamily family) {
        return family != null ? perkPoints[family.ordinal()] : 0;
    }

    public void setLevel(int index, int value) { if (index >= 0 && index < STAT_COUNT) levels[index] = value; }
    public void setXp(int index, int value) { if (index >= 0 && index < STAT_COUNT) xp[index] = value; }
    public void setPerkPoints(int index, int value) {
        StatType stat = StatType.byIndex(index);
        if (stat != null) {
            setPerkPointsForFamily(stat.family(), value);
        }
    }

    public void setPerkPointsForFamily(StatFamily family, int value) {
        if (family != null) {
            perkPoints[family.ordinal()] = Math.max(0, value);
        }
    }

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
    public void setFreeGrantedPerks(int[] ids) { freeGrantedPerks = ids.clone(); }

    public void clearUnlockedPerks() {
        unlockedPerks = new int[0];
        freeGrantedPerks = new int[0];
    }

    public void removeUnlockedPerk(int perkId) {
        if (unlockedPerks.length == 0) return;

        int count = 0;
        for (int id : unlockedPerks) {
            if (id != perkId) count++;
        }
        if (count == unlockedPerks.length) return;

        int[] next = new int[count];
        int idx = 0;
        for (int id : unlockedPerks) {
            if (id == perkId) continue;
            next[idx++] = id;
        }
        unlockedPerks = next;
        freeGrantedPerks = removeFromArray(freeGrantedPerks, perkId);
    }

    public boolean isPerkFreeGranted(int perkId) {
        for (int id : freeGrantedPerks) if (id == perkId) return true;
        return false;
    }

    public void markPerkFreeGranted(int perkId) {
        addUnlockedPerk(perkId);
        if (isPerkFreeGranted(perkId)) return;
        int[] next = new int[freeGrantedPerks.length + 1];
        System.arraycopy(freeGrantedPerks, 0, next, 0, freeGrantedPerks.length);
        next[freeGrantedPerks.length] = perkId;
        freeGrantedPerks = next;
    }

    public void markPerkPaid(int perkId) {
        freeGrantedPerks = removeFromArray(freeGrantedPerks, perkId);
    }

    public int getSoulLevel() { return soulLevel; }

    public void setSoulLevel(int level) { soulLevel = Math.max(0, level); }

    public int getArcanePoints() { return arcanePoints; }
    public void setArcanePoints(int v) { arcanePoints = Math.max(0, v); }
    public void addArcanePoints(int delta) { arcanePoints = Math.max(0, arcanePoints + delta); }

    public int getSchoolPoints(tong.statmod.magic.MagicBranch b) {
        return b == null ? 0 : schoolPoints[b.ordinal()];
    }
    public int[] getSchoolPointsArray() { return schoolPoints.clone(); }
    public void setSchoolPoints(tong.statmod.magic.MagicBranch b, int v) {
        if (b != null) schoolPoints[b.ordinal()] = Math.max(0, v);
    }
    public void addSchoolPoints(tong.statmod.magic.MagicBranch b, int delta) {
        if (b != null) schoolPoints[b.ordinal()] = Math.max(0, schoolPoints[b.ordinal()] + delta);
    }

    public int getSchoolMasteryProgress(tong.statmod.magic.MagicBranch b) {
        return b == null ? 0 : schoolMasteryProgress[b.ordinal()];
    }
    public void setSchoolMasteryProgress(tong.statmod.magic.MagicBranch b, int v) {
        if (b != null) schoolMasteryProgress[b.ordinal()] = Math.max(0, v);
    }
    public void addSchoolMasteryProgress(tong.statmod.magic.MagicBranch b, int delta) {
        if (b != null) schoolMasteryProgress[b.ordinal()] = Math.max(0, schoolMasteryProgress[b.ordinal()] + delta);
    }

    public String[] getMagicNodes() { return magicNodes.clone(); }
    public void setMagicNodes(String[] ids) { magicNodes = ids.clone(); }
    public boolean hasMagicNode(String id) {
        for (String s : magicNodes) if (s.equals(id)) return true;
        return false;
    }
    public boolean addMagicNode(String id) {
        if (id == null || hasMagicNode(id)) return false;
        String[] next = new String[magicNodes.length + 1];
        System.arraycopy(magicNodes, 0, next, 0, magicNodes.length);
        next[magicNodes.length] = id;
        magicNodes = next;
        return true;
    }

    public String[] getLearnedSpells() { return learnedSpells.clone(); }
    public void setLearnedSpells(String[] ids) { learnedSpells = ids.clone(); }
    public boolean hasLearnedSpell(String id) {
        for (String s : learnedSpells) if (s.equals(id)) return true;
        return false;
    }
    public boolean learnSpell(String id) {
        if (id == null || hasLearnedSpell(id)) return false;
        String[] next = new String[learnedSpells.length + 1];
        System.arraycopy(learnedSpells, 0, next, 0, learnedSpells.length);
        next[learnedSpells.length] = id;
        learnedSpells = next;
        return true;
    }

    public tong.statmod.magic.MagicRace getMagicRace() { return magicRace; }
    public void setMagicRace(tong.statmod.magic.MagicRace race) { magicRace = race; }

    public tong.statmod.magic.MagicBranch getChosenStartBranch() { return chosenStartBranch; }
    public void setChosenStartBranch(tong.statmod.magic.MagicBranch b) { chosenStartBranch = b; }

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
        StatType stat = StatType.byIndex(index);
        if (stat != null) {
            addPerkPointsForFamily(stat.family(), amount);
        }
    }

    public void addPerkPointsForFamily(StatFamily family, int amount) {
        if (family != null) {
            int familyIndex = family.ordinal();
            perkPoints[familyIndex] = Math.max(0, perkPoints[familyIndex] + amount);
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

    private static int[] removeFromArray(int[] source, int target) {
        if (source.length == 0) {
            return source;
        }

        int count = 0;
        for (int id : source) {
            if (id != target) count++;
        }
        if (count == source.length) {
            return source;
        }

        int[] next = new int[count];
        int index = 0;
        for (int id : source) {
            if (id == target) continue;
            next[index++] = id;
        }
        return next;
    }
}
