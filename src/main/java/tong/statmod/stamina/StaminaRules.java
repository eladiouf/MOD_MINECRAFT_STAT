package tong.statmod.stamina;

public final class StaminaRules {
    public static final float BASE_MAX_STAMINA = 100.0f;
    public static final float ENDURANCE_BONUS_PER_LEVEL = 1.0f;
    public static final float LOW_THRESHOLD_RATIO = 0.35f;
    public static final float CRITICAL_THRESHOLD_RATIO = 0.15f;

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
}
