package tong.statmod.stamina;

public class StaminaData {
    private float currentStamina = StaminaRules.BASE_MAX_STAMINA;
    private float fatigueDebt;
    private boolean meditating;

    public float currentStamina() {
        return currentStamina;
    }

    public void setCurrentStamina(float value) {
        currentStamina = sanitizeNonNegative(value);
    }

    public void copyFrom(StaminaData source) {
        if (source == null || source == this) {
            return;
        }
        setCurrentStamina(source.currentStamina);
        setFatigueDebt(source.fatigueDebt);
        meditating = source.meditating;
    }

    public float fatigueDebt() {
        return fatigueDebt;
    }

    public void setFatigueDebt(float value) {
        fatigueDebt = sanitizeNonNegative(value);
    }

    public boolean meditating() {
        return meditating;
    }

    public void setMeditating(boolean value) {
        meditating = value;
    }

    private static float sanitizeNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
