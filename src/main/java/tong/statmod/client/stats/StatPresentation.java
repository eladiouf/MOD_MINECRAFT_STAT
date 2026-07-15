package tong.statmod.client.stats;

import java.util.EnumSet;
import java.util.Set;
import tong.statmod.stats.StatType;

public record StatPresentation(
        String nameKey, String descriptionKey, StatDisplayState state) {
    private static final Set<StatType> FOUNDATION = EnumSet.of(
            StatType.ARCANE_POWER,
            StatType.CASTING_SPEED,
            StatType.MANA_POOL,
            StatType.ERUDITION,
            StatType.MAGIC_RESISTANCE);

    public static StatPresentation of(StatType type) {
        return new StatPresentation(
                "stat.statmod." + type.id(),
                "stat.statmod." + type.id() + ".description",
                FOUNDATION.contains(type) ? StatDisplayState.FOUNDATION : StatDisplayState.ACTIVE);
    }
}
