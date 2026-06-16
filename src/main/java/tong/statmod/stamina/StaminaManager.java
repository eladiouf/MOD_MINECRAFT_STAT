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

    public static void tickPassive(StaminaData data, int enduranceLevel, boolean meditating, boolean sleeping) {
        if (data == null) {
            return;
        }
        if (sleeping) {
            restore(data, 0.35f, enduranceLevel);
            return;
        }
        restore(data, StaminaRules.passiveRecoveryPerTick(meditating), enduranceLevel);
        consumeUnchecked(data, StaminaRules.passiveDrainPerTick(false, false));
    }

    public static void consumeUnchecked(StaminaData data, float amount) {
        if (data == null || amount <= 0.0f) {
            return;
        }
        data.setCurrentStamina(data.currentStamina() - amount);
    }
}
