package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientStatCache {
    private static int[] levels = new int[0];
    private static int[] xp = new int[0];
    private static int soulLevel;

    private ClientStatCache() {}

    public static void updateAll(int[] newLevels, int[] newXp, int newSoulLevel) {
        levels = newLevels.clone();
        xp = newXp.clone();
        soulLevel = newSoulLevel;
    }

    public static int getLevel(int index) {
        return index >= 0 && index < levels.length ? levels[index] : 0;
    }

    public static int getXp(int index) {
        return index >= 0 && index < xp.length ? xp[index] : 0;
    }

    public static int getSoulLevel() { return soulLevel; }

    public static int[] getLevels() { return levels.clone(); }
    public static int[] getXp() { return xp.clone(); }
}
