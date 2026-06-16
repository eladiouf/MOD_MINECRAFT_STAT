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
        currentStamina = newCurrentStamina;
        fatigueDebt = newFatigueDebt;
        meditating = newMeditating;
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
}
