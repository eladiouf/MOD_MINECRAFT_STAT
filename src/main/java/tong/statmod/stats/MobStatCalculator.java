package tong.statmod.stats;

public class MobStatCalculator {

    /** BRUTE_FORCE: multiplicative damage bonus. level 100 → +2.0 (3x total). */
    public static float getDamageBonusMultiplier(int level) {
        return level / 100.0f * 2.0f;
    }

    /** PHYSICAL_ENDURANCE: flat HP bonus. level 100 → +50 HP. */
    public static float getHealthBonusFlat(int level) {
        return level * 0.5f;
    }

    /** AGILITY + RAPIDITE: combined multiplicative speed bonus. */
    public static float getSpeedBonus(int level) {
        return level / 100.0f * 0.3f;
    }

    /** PHYSICAL_RESISTANCE: damage reduction [0, 0.5]. */
    public static float getDamageReduction(int level) {
        return Math.min(level / 200.0f, 0.5f);
    }

    /** TRACKING: detection range bonus in blocks. */
    public static float getFollowRangeBonus(int level) {
        return level * 0.4f;
    }

    /** ARCANE_POWER: magic damage multiplier. */
    public static float getMagicDamageMultiplier(int level) {
        return level / 100.0f * 1.5f;
    }

    /** FIRE/WATER/EARTH/AIR_AFFINITY: elemental resistance [0, 0.5]. */
    public static float getElementalResistance(int level) {
        return Math.min(level / 200.0f, 0.5f);
    }

    /** MAGIC_RESISTANCE: magic damage reduction [0, 0.5]. */
    public static float getMagicResistance(int level) {
        return Math.min(level / 200.0f, 0.5f);
    }

    /** WILLPOWER: knockback resistance [0, 1]. */
    public static float getKnockbackResistance(int level) {
        return Math.min(level / 100.0f, 1.0f);
    }

    /** INTIMIDATION: slowness aura radius in blocks. */
    public static float getIntimidationRadius(int level) {
        return level / 20.0f;
    }

    /** MANA_POOL: mob max mana for skills. */
    public static int getMobMaxMana(int level) {
        return level * 2;
    }
}
