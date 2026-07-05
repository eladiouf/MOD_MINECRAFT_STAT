package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientStatCache {
    private static int[] levels = new int[0];
    private static int[] xp = new int[0];
    private static int soulLevel;
    private static int dungeonPoints;
    private static int dungeonFloorReached = 1;

    private ClientStatCache() {}

    public static void updateAll(int[] newLevels, int[] newXp, int newSoulLevel,
                                 int newDungeonPoints, int newDungeonFloorReached) {
        levels = sanitizedCopy(newLevels);
        xp = sanitizedCopy(newXp);
        soulLevel = Math.max(0, newSoulLevel);
        dungeonPoints = Math.max(0, newDungeonPoints);
        dungeonFloorReached = Math.max(1, newDungeonFloorReached);
    }

    public static void reset() {
        levels = new int[0];
        xp = new int[0];
        soulLevel = 0;
        dungeonPoints = 0;
        dungeonFloorReached = 1;
    }

    public static int getDungeonPoints() { return dungeonPoints; }
    public static int getDungeonFloorReached() { return dungeonFloorReached; }

    public static int getLevel(int index) {
        return index >= 0 && index < levels.length ? levels[index] : 0;
    }

    public static int getXp(int index) {
        return index >= 0 && index < xp.length ? xp[index] : 0;
    }

    public static boolean hasLevel(int index) {
        return index >= 0 && index < levels.length;
    }

    public static int getSoulLevel() { return soulLevel; }

    public static int[] getLevels() { return levels.clone(); }
    public static int[] getXp() { return xp.clone(); }

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
