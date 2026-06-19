package tong.statmod.stamina;

public final class StaminaRules {
    public static final float BASE_MAX_STAMINA = 100.0f;
    public static final float ENDURANCE_BONUS_PER_LEVEL = 1.0f;
    public static final float LOW_THRESHOLD_RATIO = 0.35f;
    public static final float CRITICAL_THRESHOLD_RATIO = 0.15f;
    public static final float MIN_RECOVERY_MULTIPLIER = 0.35f;

    private StaminaRules() {}

    public static float maxStamina(int enduranceLevel) {
        return BASE_MAX_STAMINA + Math.max(0, enduranceLevel) * ENDURANCE_BONUS_PER_LEVEL;
    }

    public static StaminaThreshold threshold(float current, float max) {
        if (max <= 0.0f) {
            return StaminaThreshold.CRITICAL;
        }

        float ratio = Math.max(0.0f, current) / max;
        if (ratio <= CRITICAL_THRESHOLD_RATIO) return StaminaThreshold.CRITICAL;
        if (ratio <= LOW_THRESHOLD_RATIO) return StaminaThreshold.LOW;
        return StaminaThreshold.NORMAL;
    }

    public static float passiveRecoveryPerTick(boolean meditating) {
        return meditating ? 0.18f : 0.02f;
    }

    public static float passiveDrainPerTick(boolean sprinting, boolean airborne) {
        float drain = 0.0025f;
        if (sprinting) drain += 0.08f;
        if (airborne) drain += 0.02f;
        return drain;
    }

    public static float foodRecoveryAmount(int nutrition, float saturationModifier) {
        return Math.max(0.0f, nutrition * 1.5f + saturationModifier * 4.0f);
    }

    public static float fatigueDebtFromSpend(float spentStamina) {
        return Math.max(0.0f, spentStamina) * 0.25f;
    }

    public static float fatigueReliefPerTick(boolean meditating, boolean sleeping) {
        if (sleeping) {
            return 0.12f;
        }
        return meditating ? 0.05f : 0.005f;
    }

    public static float foodFatigueRelief(int nutrition, float saturationModifier) {
        return Math.max(0.0f, nutrition * 0.5f + saturationModifier);
    }

    public static float wakeFatigueRelief() {
        return 12.0f;
    }

    public static float recoveryMultiplierFromDebt(float fatigueDebt, int enduranceLevel) {
        float max = Math.max(1.0f, maxStamina(enduranceLevel));
        float ratio = Math.min(1.0f, Math.max(0.0f, fatigueDebt) / max);
        return Math.max(MIN_RECOVERY_MULTIPLIER, 1.0f - ratio * 0.65f);
    }
}
