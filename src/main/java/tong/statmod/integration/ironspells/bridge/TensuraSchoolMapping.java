package tong.statmod.integration.ironspells.bridge;

import tong.statmod.stats.StatType;

/**
 * Mapping pure des StatType Tensura vers les écoles Iron's Spellbooks.
 *
 * <p>Les noms d'école sont exprimés comme tokens (path component du
 * {@code ResourceLocation}) pour rester découplés de l'API runtime Iron's Spellbooks
 * et permettre des tests offline. La résolution effective vers
 * {@code SchoolType} se fait à l'intérieur de {@link TensuraDelegatingSpell}.
 */
public final class TensuraSchoolMapping {
    /** Iron's Spellbooks namespace for built-in schools. */
    public static final String IRONS_NS = "irons_spellbooks";

    private TensuraSchoolMapping() {}

    /**
     * Returns the Iron's Spellbooks school path that best matches a Tensura primary stat.
     * Always returns a non-null built-in school; defaults to {@code evocation}.
     */
    public static String schoolPathFor(StatType primaryStat) {
        if (primaryStat == null) return "evocation";
        return switch (primaryStat) {
            case FIRE_AFFINITY -> "fire";
            case WATER_AFFINITY -> "ice";
            case EARTH_AFFINITY -> "nature";
            case AIR_AFFINITY -> "lightning";
            case ARCANE_POWER, MANA_POOL, CASTING_SPEED -> "evocation";
            case ERUDITION, MAGIC_RESISTANCE -> "holy";
            default -> "evocation";
        };
    }
}
