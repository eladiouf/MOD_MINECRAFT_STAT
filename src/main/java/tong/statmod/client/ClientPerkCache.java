package tong.statmod.client;

import tong.statmod.perks.Perk;

import java.util.HashSet;
import java.util.Set;

public class ClientPerkCache {
    private static final Set<Integer> unlockedPerks = new HashSet<>();
    private static int availablePoints = 0;

    public static void update(int[] perkIds, int points) {
        unlockedPerks.clear();
        for (int id : perkIds) unlockedPerks.add(id);
        availablePoints = points;
    }

    public static boolean isUnlocked(Perk perk) { return unlockedPerks.contains(perk.id); }
    public static boolean isUnlocked(int perkId) { return unlockedPerks.contains(perkId); }
    public static int getAvailablePoints() { return availablePoints; }
}
