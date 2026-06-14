package tong.statmod.stats;

import tong.statmod.Config;

public class StatCalculator {

    public static int getXpForNextLevel(int level) {
        return (int) Math.round(Config.xpBaseCost * Math.pow(Config.xpGrowthFactor, level));
    }

    // Brute Force: +0.4%×√(level) per level (max +40% at lvl 100)
    public static float getDamageBonus(int level) {
        return 0.04f * (float) Math.sqrt(level);
    }

    // Blade Technique: +0.3%×√(level) per level (max +30% at lvl 100)
    public static float getBladeDamageBonus(int level) {
        return 0.03f * (float) Math.sqrt(level);
    }

    // Physical Resistance: -0.5%×√(level) per level (max -50% at lvl 100)
    public static float getDamageReduction(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Rapidité: +0.5%×√(level) attack speed per level (max +50% at lvl 100)
    public static float getAttackSpeedBonus(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Agility: +0.4%×√(level) move speed per level (max +40% at lvl 100)
    public static float getMoveSpeedBonus(int level) {
        return 0.04f * (float) Math.sqrt(level);
    }

    // Physical Endurance: +0.5×√(level) hearts per level (max +50 hearts at lvl 100)
    public static float getEnduranceHearts(int level) {
        return 5.0f * (float) Math.sqrt(level);
    }

    // Precision: +0.5%×√(level) crit chance per level (max +50% at lvl 100)
    public static float getCritChance(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Arcane Power: +0.6%×√(level) magic damage per level (max +60% at lvl 100)
    public static float getMagicDamageBonus(int level) {
        return 0.06f * (float) Math.sqrt(level);
    }

    // Water Affinity: +0.8%×√(level) swim speed per level (max +80% at lvl 100)
    public static float getSwimSpeedBonus(int level) {
        return 0.08f * (float) Math.sqrt(level);
    }

    // Earth Affinity: +0.6%×√(level) mining speed per level (max +60% at lvl 100)
    public static float getMiningSpeedBonus(int level) {
        return 0.06f * (float) Math.sqrt(level);
    }

    // Fire Affinity: +1.0%×√(level) fire damage per level (max +100% at lvl 100)
    public static float getFireDamageBonus(int level) {
        return 0.10f * (float) Math.sqrt(level);
    }

    // Air Affinity: +0.5%×√(level) jump height per level (max +50% at lvl 100)
    public static float getJumpBonus(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Magic Resistance: -0.5%×√(level) magic damage taken per level (max -50% at lvl 100)
    public static float getMagicReduction(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Casting Speed: +0.5%×√(level) item use speed per level (max +50% at lvl 100)
    public static float getItemUseSpeed(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Mana Pool: +2×√(level) max mana per level (max +200 at lvl 100)
    public static int getManaBonus(int level) {
        return (int) Math.round(20.0 * Math.sqrt(level));
    }

    // Erudition: +1.0%×√(level) XP bonus per level (max +100% at lvl 100)
    public static float getXpBonus(int level) {
        return 0.10f * (float) Math.sqrt(level);
    }

    // Forging: +0.5%×√(level) tool durability per level (max +50% at lvl 100)
    public static float getDurabilityBonus(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Cooking: +0.5%×√(level) food saturation per level (max +50% at lvl 100)
    public static float getSaturationBonus(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Alchemy: +0.5%×√(level) potion duration per level (max +50% at lvl 100)
    public static float getPotionDurationBonus(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Willpower: -0.8%×√(level) status duration per level (max -80% at lvl 100)
    public static float getStatusDurationReduction(int level) {
        return 0.08f * (float) Math.sqrt(level);
    }

    // Fatigue reduction (Willpower)
    public static float getFatigueReduction(int level) {
        return 0.08f * (float) Math.sqrt(level);
    }

    // Tracking: +0.5%×√(level) luck per level (max +50% at lvl 100)
    public static float getLuckBonus(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Keen Senses: +0.5×√(level) blocks detection radius (max +50 blocks, base 5)
    public static float getDetectionRadius(int level) {
        return 5.0f + 5.0f * (float) Math.sqrt(level);
    }

    // Intimidation: fear range +0.8×√(level) blocks (base 2, max +80 at lvl 100)
    public static float getFearRange(int level) {
        return 2.0f + 8.0f * (float) Math.sqrt(level);
    }

    // Crafting: extra nutrition (√(level)×0.6, min 1, max 6)
    public static int getExtraNutrition(int level) {
        return Math.max(1, (int) (Math.sqrt(level) * 0.6));
    }

    // Water Affinity: extra underwater damage (+0.5%×√(level), max +50% at lvl 100)
    public static float getUnderwaterDamageBonus(int level) {
        return 0.05f * (float) Math.sqrt(level);
    }

    // Forging: tool attack damage (+0.3%×√(level), max +30% at lvl 100)
    public static float getToolDamageBonus(int level) {
        return 0.03f * (float) Math.sqrt(level);
    }

    // Forging: double drop chance on ores (+0.2%×√(level), max +20% at lvl 100)
    public static float getDoubleDropChance(int level) {
        return 0.02f * (float) Math.sqrt(level);
    }

    // Cooking: food buff duration multiplier (×1.1 at lvl 10, ×2 at lvl 100)
    public static float getFoodBuffDuration(int level) {
        return 1.0f + 0.10f * (float) Math.sqrt(level);
    }

    // Keen Senses: dodge chance (+0.1%×√(level), max +10% at lvl 100)
    public static float getDodgeChance(int level) {
        return 0.01f * (float) Math.sqrt(level);
    }

    // Keen Senses: combat XP bonus (+0.5%×√(level), max +50% at lvl 100 → 1.5×)
    public static float getCombatXpRegenBoost(int level) {
        return 1.0f + 0.05f * (float) Math.sqrt(level);
    }
}
