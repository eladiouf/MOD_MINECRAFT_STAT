package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientManaCache {
    private static float currentMana;
    private static float maxMana;

    private ClientManaCache() {}

    public static void update(float newCurrentMana, float newMaxMana) {
        currentMana = sanitizeNonNegative(newCurrentMana);
        maxMana = sanitizeNonNegative(newMaxMana);
    }

    public static void reset() {
        currentMana = 0.0f;
        maxMana = 0.0f;
    }

    public static float getCurrentMana() {
        return currentMana;
    }

    public static float getMaxMana() {
        return maxMana;
    }

    private static float sanitizeNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
