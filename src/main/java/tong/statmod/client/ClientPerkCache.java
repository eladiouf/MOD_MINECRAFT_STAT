package tong.statmod.client;

import tong.statmod.perks.Perk;

import java.util.HashSet;
import java.util.Set;

public class ClientPerkCache {
    private static final Set<Integer> unlockedPerks = new HashSet<>();
    private static int[] perStatPoints = new int[23];

    public static void update(int[] perkIds, int[] points) {
        unlockedPerks.clear();
        for (int id : perkIds) unlockedPerks.add(id);
        perStatPoints = points.clone();
    }

    public static boolean isUnlocked(Perk perk) { return unlockedPerks.contains(perk.id); }
    public static boolean isUnlocked(int perkId) { return unlockedPerks.contains(perkId); }
    public static int getPointsForStat(int statIndex) {
        if (statIndex < 0 || statIndex >= perStatPoints.length) return 0;
        return perStatPoints[statIndex];
    }
    public static int getSpentPointsInStat(int statIndex) {
        int spent = 0;
        for (int id : unlockedPerks) {
            Perk p = Perk.byId(id);
            if (p != null && p.stat.index == statIndex) spent += p.tier.cost;
        }
        return spent;
    }
    public static int getAvailablePointsForStat(int statIndex) {
        return Math.max(0, getPointsForStat(statIndex) - getSpentPointsInStat(statIndex));
    }
}
