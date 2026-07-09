package tong.statmod.progression;

final class ActivityXpScaling {
    private static final double CLOSE_THREAT_KILL_DISTANCE = 4.0d;

    private ActivityXpScaling() {}

    static int agilityXpForMovement(boolean moving, boolean sprinting, boolean flying, boolean passenger) {
        return moving && sprinting && !flying && !passenger ? 1 : 0;
    }

    static int enduranceXpForMovement(boolean moving,
                                      boolean sprinting,
                                      boolean swimming,
                                      boolean flying,
                                      boolean passenger) {
        return moving && (sprinting || swimming) && !flying && !passenger ? 1 : 0;
    }

    static int enduranceXpForPhysicalDamage(float originalDamage) {
        if (originalDamage <= 0.0f) {
            return 0;
        }
        return Math.min(6, Math.max(1, Math.round(originalDamage / 6.0f)));
    }

    static int intimidationXpForKill(double distanceToTarget, boolean targetWasThreatening, float targetMaxHealth) {
        if (!targetWasThreatening || targetMaxHealth <= 0.0f || distanceToTarget > CLOSE_THREAT_KILL_DISTANCE) {
            return 0;
        }
        return Math.min(6, Math.max(1, Math.round(targetMaxHealth / 20.0f)));
    }
}
