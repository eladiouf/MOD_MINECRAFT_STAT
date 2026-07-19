package tong.statmod.config;

public final class Config {
    private Config() {}

    public static double getDungeonXpBaseMultiplier() {
        return 1.0;
    }

    public static double getDungeonXpPerFloor() {
        return 0.02;
    }

    public static int getDungeonBossStatGain() {
        return 2;
    }

    public static double getPointToCoinRate() {
        return 1.0;
    }

    public static double getDungeonHostilityPerFloor() {
        return 0.0;
    }

    public static int getDungeonHostilityCap() {
        return 100;
    }
}
