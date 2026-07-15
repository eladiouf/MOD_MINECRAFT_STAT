package tong.statmod.client.stats;

import tong.statmod.stats.StatType;

public record StatPresentation(
        String nameKey, String descriptionKey, StatDisplayState state) {
    public static StatPresentation of(StatType type) {
        return new StatPresentation(
                "stat.statmod." + type.id(),
                "stat.statmod." + type.id() + ".description",
                StatDisplayState.ACTIVE);
    }
}
