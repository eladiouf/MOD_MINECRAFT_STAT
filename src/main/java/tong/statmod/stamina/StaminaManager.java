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
        data.setFatigueDebt(data.fatigueDebt() + StaminaRules.fatigueDebtFromSpend(amount));
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
        tickPassive(data, enduranceLevel, meditating, sleeping, false, false);
    }

    public static void tickPassive(StaminaData data, int enduranceLevel, boolean meditating, boolean sleeping,
                                   boolean sprinting, boolean airborne) {
        if (data == null) {
            return;
        }
        if (sleeping) {
            restore(data, 0.35f, enduranceLevel);
            relieveFatigue(data, StaminaRules.fatigueReliefPerTick(false, true));
            return;
        }
        float recovery = StaminaRules.passiveRecoveryPerTick(meditating)
                * StaminaRules.recoveryMultiplierFromDebt(data.fatigueDebt(), enduranceLevel);
        restore(data, recovery, enduranceLevel);
        consumeUnchecked(data, StaminaRules.passiveDrainPerTick(sprinting, airborne));
        relieveFatigue(data, StaminaRules.fatigueReliefPerTick(meditating, false));
    }

    public static void consumeUnchecked(StaminaData data, float amount) {
        if (data == null || amount <= 0.0f) {
            return;
        }
        data.setCurrentStamina(data.currentStamina() - amount);
        data.setFatigueDebt(data.fatigueDebt() + StaminaRules.fatigueDebtFromSpend(amount));
    }

    public static void relieveFatigue(StaminaData data, float amount) {
        if (data == null || amount <= 0.0f) {
            return;
        }
        data.setFatigueDebt(data.fatigueDebt() - amount);
    }
}
