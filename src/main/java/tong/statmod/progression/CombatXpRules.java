package tong.statmod.progression;

final class CombatXpRules {
    private static final float HEART_THRESHOLD = 1.0f;

    private CombatXpRules() {
    }

    static boolean isLowHealthAfterHit(float currentHealth, float damage, float maxHealth, float thresholdFraction) {
        if (maxHealth <= 0) return false;
        float remainingHealth = Math.max(0f, currentHealth - damage);
        return remainingHealth / maxHealth < thresholdFraction;
    }

    static boolean survivesAtHalfHeart(float currentHealth, float damage) {
        float remainingHealth = Math.max(0f, currentHealth - damage);
        return currentHealth > 0f && remainingHealth <= HEART_THRESHOLD;
    }
}
