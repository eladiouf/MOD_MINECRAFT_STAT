package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientStaminaCache {
    private static float currentStamina;
    private static float fatigueDebt;
    private static boolean meditating;

    private ClientStaminaCache() {}

    public static void update(float newCurrentStamina, float newFatigueDebt, boolean newMeditating) {
        currentStamina = sanitizeNonNegative(newCurrentStamina);
        fatigueDebt = sanitizeNonNegative(newFatigueDebt);
        meditating = newMeditating;
    }

    public static void reset() {
        currentStamina = 0.0f;
        fatigueDebt = 0.0f;
        meditating = false;
    }

    public static float getCurrentStamina() {
        return currentStamina;
    }

    public static float getFatigueDebt() {
        return fatigueDebt;
    }

    public static boolean isMeditating() {
        return meditating;
    }

    private static float sanitizeNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
