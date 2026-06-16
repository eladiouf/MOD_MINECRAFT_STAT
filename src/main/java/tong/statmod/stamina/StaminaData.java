package tong.statmod.stamina;

public class StaminaData {
    private float currentStamina = StaminaRules.BASE_MAX_STAMINA;
    private float fatigueDebt;
    private boolean meditating;

    public float currentStamina() {
        return currentStamina;
    }

    public void setCurrentStamina(float value) {
        currentStamina = Math.max(0.0f, value);
    }

    public float fatigueDebt() {
        return fatigueDebt;
    }

    public void setFatigueDebt(float value) {
        fatigueDebt = Math.max(0.0f, value);
    }

    public boolean meditating() {
        return meditating;
    }

    public void setMeditating(boolean value) {
        meditating = value;
    }
}
