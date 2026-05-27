package tong.statmod.stats;

public class StatCalculator {

    public static int getXpForNextLevel(int level) {
        return (level + 1) * (level + 1) * 10;
    }

    public static float getDamageBonus(int level) {
        return 0.01f * level;
    }

    public static float getDamageReduction(int resistanceLevel) {
        return 0.005f * resistanceLevel;
    }

    public static float getAttackSpeedBonus(int rapLevel) {
        return 0.005f * rapLevel;
    }

    public static float getMoveSpeedBonus(int agilityLevel) {
        return 0.003f * agilityLevel;
    }

    public static float getCritChance(int precisionLevel) {
        return 0.005f * precisionLevel;
    }

    public static float getEnduranceHearts(int enduranceLevel) {
        return 0.2f * enduranceLevel;
    }

    public static float getFatigueReduction(int willpowerLevel) {
        return 0.005f * willpowerLevel;
    }
}
