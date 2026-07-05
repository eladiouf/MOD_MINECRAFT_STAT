package tong.statmod.integration;

import tong.statmod.stats.StatType;

public record RaceModifier(int statIndex, int flatBonus, double xpMultiplier) {
    public RaceModifier {
        if (StatType.byIndex(statIndex) == null) {
            throw new IllegalArgumentException("Unknown stat index " + statIndex);
        }
        xpMultiplier = Math.max(0.1, Math.min(xpMultiplier, 5.0));
    }
}
