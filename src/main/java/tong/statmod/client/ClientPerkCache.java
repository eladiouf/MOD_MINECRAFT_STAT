package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

@OnlyIn(Dist.CLIENT)
public final class ClientPerkCache {
    private static boolean[] unlocked = new boolean[Perk.values().length];
    private static int[] familyPoints = new int[0];

    private ClientPerkCache() {}

    public static void update(int[] perkIds, int[] points) {
        boolean[] next = new boolean[Perk.values().length];
        if (perkIds != null) {
            for (int id : perkIds) {
                if (id >= 0 && id < next.length) next[id] = true;
            }
        }
        unlocked = next;
        familyPoints = sanitizedCopy(points);
    }

    public static void reset() {
        unlocked = new boolean[Perk.values().length];
        familyPoints = new int[0];
    }

    public static boolean isUnlocked(Perk perk) {
        return perk != null && perk.id < unlocked.length && unlocked[perk.id];
    }

    public static int getPointsForStat(int statIndex) {
        StatType stat = StatType.byIndex(statIndex);
        return stat != null ? getPointsForFamily(stat.family().ordinal()) : 0;
    }

    public static int getPointsForFamily(int familyIndex) {
        return familyIndex >= 0 && familyIndex < familyPoints.length ? familyPoints[familyIndex] : 0;
    }

    public static int[] getPerStatPoints() { return familyPoints.clone(); }

    private static int[] sanitizedCopy(int[] source) {
        if (source == null || source.length == 0) {
            return new int[0];
        }
        int[] copy = new int[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = Math.max(0, source[i]);
        }
        return copy;
    }
}
