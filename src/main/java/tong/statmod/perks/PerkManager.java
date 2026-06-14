package tong.statmod.perks;

import tong.statmod.storage.PlayerStatData;

import java.util.BitSet;

public class PerkManager {
    private static final int PERK_COUNT = 84;
    private final BitSet unlocked = new BitSet(PERK_COUNT);
    private final PlayerStatData statData;

    public PerkManager(PlayerStatData statData) {
        this.statData = statData;
    }

    public boolean isUnlocked(Perk perk) {
        return perk != null && unlocked.get(perk.id);
    }

    public boolean[] getUnlockedArray() {
        boolean[] arr = new boolean[PERK_COUNT];
        for (int i = 0; i < PERK_COUNT; i++) arr[i] = unlocked.get(i);
        return arr;
    }

    public void setFromArray(boolean[] arr) {
        unlocked.clear();
        for (int i = 0; i < Math.min(arr.length, PERK_COUNT); i++) {
            if (arr[i]) unlocked.set(i);
        }
    }

    public int[] getUnlockedIds() {
        return unlocked.stream().toArray();
    }

    public void setFromIds(int[] ids) {
        unlocked.clear();
        for (int id : ids) if (id >= 0 && id < PERK_COUNT) unlocked.set(id);
    }

    public int getPointsForStat(int statIndex) {
        return statData.getPerkPointsForStat(statIndex);
    }

    public boolean canUnlock(Perk perk) {
        if (perk == null || isUnlocked(perk)) return false;
        int statLevel = statData.getLevel(perk.stat.index);
        if (statLevel < perk.tier.requiredStatLevel) return false;
        if (getPointsForStat(perk.stat.index) < perk.tier.cost) return false;
        if (perk.synergyStat != null) {
            int synergyLevel = statData.getLevel(perk.synergyStat.index);
            if (synergyLevel < PerkTier.SYNERGY.requiredStatLevel) return false;
        }
        return true;
    }

    public boolean unlock(Perk perk) {
        if (!canUnlock(perk)) return false;
        unlocked.set(perk.id);
        statData.addPerkPointsForStat(perk.stat.index, -perk.tier.cost);
        return true;
    }

    public void resetAll() {
        unlocked.clear();
    }
}
