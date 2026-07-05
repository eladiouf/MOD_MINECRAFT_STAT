package tong.statmod.integration.epicfight;

import tong.statmod.stamina.StaminaThreshold;

public final class EpicFightStaminaBridge {
    private static final float MAX_SKILL_COST_RATIO = 0.04f;

    private EpicFightStaminaBridge() {}

    public static boolean canUseSkill(StaminaThreshold threshold, float current, float cost) {
        return cost <= 0.0f || current >= cost;
    }

    public static float damageMultiplier(StaminaThreshold threshold) {
        if (threshold == StaminaThreshold.LOW) return 0.9f;
        if (threshold == StaminaThreshold.CRITICAL) return 0.8f;
        return 1.0f;
    }

    public static float attackSpeedMultiplier(StaminaThreshold threshold) {
        if (threshold == StaminaThreshold.LOW) return 0.92f;
        if (threshold == StaminaThreshold.CRITICAL) return 0.84f;
        return 1.0f;
    }

    public static float sustainableSkillCost(float rawCost, float resourceMultiplier, float statModMax) {
        if (rawCost <= 0.0f || statModMax <= 0.0f) {
            return 0.0f;
        }
        float adjustedCost = rawCost * Math.max(0.0f, resourceMultiplier);
        return Math.min(adjustedCost, statModMax * MAX_SKILL_COST_RATIO);
    }

    public static float patchDisplayStamina(float current, float statModMax, float epicFightMax) {
        if (statModMax <= 0.0f || epicFightMax <= 0.0f) {
            return 0.0f;
        }
        float ratio = Math.max(0.0f, Math.min(1.0f, current / statModMax));
        return ratio * epicFightMax;
    }
}
