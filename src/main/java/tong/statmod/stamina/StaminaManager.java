package tong.statmod.stamina;

public final class StaminaManager {
    private StaminaManager() {}

    public static boolean consume(StaminaData data, float amount) {
        if (data == null || amount <= 0.0f) {
            return true;
        }
        if (data.currentStamina() < amount) {
            return false;
        }
        data.setCurrentStamina(data.currentStamina() - amount);
        return true;
    }

    public static void restore(StaminaData data, float amount, int enduranceLevel) {
        if (data == null || amount <= 0.0f) {
            return;
        }
        float max = StaminaRules.maxStamina(enduranceLevel);
        data.setCurrentStamina(Math.min(max, data.currentStamina() + amount));
    }
}
