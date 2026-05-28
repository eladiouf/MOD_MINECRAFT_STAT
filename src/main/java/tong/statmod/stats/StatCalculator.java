package tong.statmod.stats;

public class StatCalculator {

    public static int getXpForNextLevel(int level) {
        return (level + 1) * (level + 1) * 10;
    }

    // Brute Force: +0.2% damage per level (max +20%)
    public static float getDamageBonus(int level) {
        return 0.002f * level;
    }

    // Blade Technique: +0.15% damage per level (max +15%)
    public static float getBladeDamageBonus(int level) {
        return 0.0015f * level;
    }

    // Physical Resistance: -0.3% damage taken per level (max -30%)
    public static float getDamageReduction(int level) {
        return 0.003f * level;
    }

    // Rapidité: +0.3% attack speed per level (max +30%)
    public static float getAttackSpeedBonus(int level) {
        return 0.003f * level;
    }

    // Agility: +0.2% move speed per level (max +20%)
    public static float getMoveSpeedBonus(int level) {
        return 0.002f * level;
    }

    // Physical Endurance: +0.2 hearts per level (max +20 hearts)
    public static float getEnduranceHearts(int level) {
        return 0.2f * level;
    }

    // Precision: +0.3% crit chance per level (max +30%)
    public static float getCritChance(int level) {
        return 0.003f * level;
    }

    // Arcane Power: +0.3% magic damage per level (max +30%)
    public static float getMagicDamageBonus(int level) {
        return 0.003f * level;
    }

    // Water Affinity: +0.5% swim speed per level (max +50%)
    public static float getSwimSpeedBonus(int level) {
        return 0.005f * level;
    }

    // Earth Affinity: +0.3% mining speed per level (max +30%)
    public static float getMiningSpeedBonus(int level) {
        return 0.003f * level;
    }

    // Fire Affinity: +0.5% fire damage per level (max +50%)
    public static float getFireDamageBonus(int level) {
        return 0.005f * level;
    }

    // Air Affinity: +0.3% jump height per level (max +30%)
    public static float getJumpBonus(int level) {
        return 0.003f * level;
    }

    // Magic Resistance: -0.3% magic damage taken per level (max -30%)
    public static float getMagicReduction(int level) {
        return 0.003f * level;
    }

    // Casting Speed: +0.3% item use speed per level (max +30%)
    public static float getItemUseSpeed(int level) {
        return 0.003f * level;
    }

    // Mana Pool: +1 max mana per level (max +100)
    public static int getManaBonus(int level) {
        return level;
    }

    // Erudition: +0.5% XP bonus per level (max +50%)
    public static float getXpBonus(int level) {
        return 0.005f * level;
    }

    // Forging: +0.3% tool durability per level (max +30%)
    public static float getDurabilityBonus(int level) {
        return 0.003f * level;
    }

    // Cooking: +0.3% food saturation per level (max +30%)
    public static float getSaturationBonus(int level) {
        return 0.003f * level;
    }

    // Alchemy: +0.3% potion duration per level (max +30%)
    public static float getPotionDurationBonus(int level) {
        return 0.003f * level;
    }

    // Intimidation: +0.3% mob fear range per level (max +30%)
    public static float getFearRange(int level) {
        return 0.003f * level;
    }

    // Willpower: -0.5% status duration per level (max -50%)
    public static float getStatusDurationReduction(int level) {
        return 0.005f * level;
    }

    // Fatigue reduction (Willpower)
    public static float getFatigueReduction(int level) {
        return 0.005f * level;
    }
}
