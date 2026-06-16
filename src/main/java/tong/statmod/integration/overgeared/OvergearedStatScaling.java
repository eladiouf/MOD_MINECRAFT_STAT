package tong.statmod.integration.overgeared;

import net.stirdrem.overgeared.ForgingQuality;

public final class OvergearedStatScaling {
    private OvergearedStatScaling() {}

    public static double xpMultiplier(ForgingQuality quality) {
        if (quality == null) {
            return 1.0;
        }
        return switch (quality) {
            case POOR -> 1.0;
            case WELL -> 1.2;
            case EXPERT -> 1.4;
            case PERFECT -> 1.6;
            case MASTER -> 1.8;
            case NONE -> 1.0;
        };
    }

    public static int scaledXp(int baseXp, ForgingQuality quality) {
        if (baseXp <= 0) {
            return 0;
        }
        return (int) Math.round(baseXp * xpMultiplier(quality));
    }

    public static float digSpeedMultiplier(int forgingLevel) {
        return 1.0f + Math.max(0, forgingLevel) * 0.02f;
    }

    public static int adjustedDurabilityDamage(int baseDamage, int forgingLevel) {
        if (baseDamage <= 0) {
            return 0;
        }
        double multiplier = Math.max(0.0, 1.0 - Math.max(0, forgingLevel) * 0.1);
        return (int) Math.round(baseDamage * multiplier);
    }
}
