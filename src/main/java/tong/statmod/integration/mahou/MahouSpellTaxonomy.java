package tong.statmod.integration.mahou;

import tong.statmod.stats.StatType;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MahouSpellTaxonomy {
    private static final Map<String, MahouSpellProfile> PROFILES = Map.ofEntries(
            Map.entry("mahoutsukai:gandr_spell_scroll", new MahouSpellProfile("mahoutsukai:gandr_spell_scroll", "arcane_offense", StatType.ARCANE_POWER, List.of(StatType.CASTING_SPEED))),
            Map.entry("mahoutsukai:rho_aias_spell_scroll", new MahouSpellProfile("mahoutsukai:rho_aias_spell_scroll", "barrier", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE, StatType.WILLPOWER))),
            Map.entry("mahoutsukai:fallen_down_spell_scroll", new MahouSpellProfile("mahoutsukai:fallen_down_spell_scroll", "cataclysm", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER))),
            Map.entry("mahoutsukai:mystic_staff_spell_scroll", new MahouSpellProfile("mahoutsukai:mystic_staff_spell_scroll", "mastery", StatType.ERUDITION, List.of(StatType.MANA_POOL))),
            Map.entry("mahoutsukai:scroll_boundary_drain_life", new MahouSpellProfile("mahoutsukai:scroll_boundary_drain_life", "ritual", StatType.ARCANE_POWER, List.of(StatType.MANA_POOL, StatType.WILLPOWER)))
    );

    private MahouSpellTaxonomy() {}

    public static MahouSpellProfile profile(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return PROFILES.get(itemId.toLowerCase(Locale.ROOT));
    }

    public static StatType primaryStat(String itemId) {
        MahouSpellProfile profile = profile(itemId);
        return profile == null ? StatType.ARCANE_POWER : profile.primaryStat();
    }

    public static List<StatType> secondaryStats(String itemId) {
        MahouSpellProfile profile = profile(itemId);
        return profile == null ? Collections.emptyList() : profile.secondaryStats();
    }
}
