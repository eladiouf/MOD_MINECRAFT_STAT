package tong.statmod.integration.mahou;

import tong.statmod.stats.StatType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MahouSpellTaxonomy {
    private static final Map<String, MahouSpellProfile> PROFILES = Map.of(
            "mahoutsukai:gandr_spell_scroll", new MahouSpellProfile("mahoutsukai:gandr_spell_scroll", "arcane_offense", StatType.ARCANE_POWER, List.of(StatType.CASTING_SPEED)),
            "mahoutsukai:rho_aias_spell_scroll", new MahouSpellProfile("mahoutsukai:rho_aias_spell_scroll", "barrier", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE, StatType.WILLPOWER)),
            "mahoutsukai:fallen_down_spell_scroll", new MahouSpellProfile("mahoutsukai:fallen_down_spell_scroll", "cataclysm", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER)),
            "mahoutsukai:mystic_staff_spell_scroll", new MahouSpellProfile("mahoutsukai:mystic_staff_spell_scroll", "mastery", StatType.ERUDITION, List.of(StatType.MANA_POOL))
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
}
