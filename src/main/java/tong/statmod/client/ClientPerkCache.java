package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.perks.Perk;

@OnlyIn(Dist.CLIENT)
public final class ClientPerkCache {
    private static boolean[] unlocked = new boolean[84];
    private static int[] perStatPoints = new int[0];

    private ClientPerkCache() {}

    public static void update(int[] perkIds, int[] points) {
        boolean[] next = new boolean[84];
        for (int id : perkIds) {
            if (id >= 0 && id < 84) next[id] = true;
        }
        unlocked = next;
        perStatPoints = points.clone();
    }

    public static boolean isUnlocked(Perk perk) {
        return perk != null && perk.id < unlocked.length && unlocked[perk.id];
    }

    public static int getPointsForStat(int statIndex) {
        return statIndex >= 0 && statIndex < perStatPoints.length ? perStatPoints[statIndex] : 0;
    }

    public static int[] getPerStatPoints() { return perStatPoints.clone(); }
}
