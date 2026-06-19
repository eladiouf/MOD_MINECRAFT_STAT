package tong.statmod.integration.mahou;

import tong.statmod.stats.StatType;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MahouSpellTaxonomy {
    private static final Map<String, MahouSpellProfile> PROFILES = Map.ofEntries(
            Map.entry("mahoutsukai:scroll_gandr", new MahouSpellProfile("mahoutsukai:scroll_gandr", "arcane_offense", StatType.ARCANE_POWER, List.of(StatType.CASTING_SPEED))),
            Map.entry("mahoutsukai:scroll_rho_aias", new MahouSpellProfile("mahoutsukai:scroll_rho_aias", "barrier", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE, StatType.WILLPOWER))),
            Map.entry("mahoutsukai:scroll_fallen_down", new MahouSpellProfile("mahoutsukai:scroll_fallen_down", "cataclysm", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER))),
            Map.entry("mahoutsukai:scroll_mystic_staff", new MahouSpellProfile("mahoutsukai:scroll_mystic_staff", "mastery", StatType.ERUDITION, List.of(StatType.MANA_POOL))),
            Map.entry("mahoutsukai:scroll_boundary_drain_life", new MahouSpellProfile("mahoutsukai:scroll_boundary_drain_life", "ritual", StatType.ARCANE_POWER, List.of(StatType.MANA_POOL, StatType.WILLPOWER)))
            ,Map.entry("mahoutsukai:scroll_mental_displacement", new MahouSpellProfile("mahoutsukai:scroll_mental_displacement", "displacement", StatType.CASTING_SPEED, List.of(StatType.AIR_AFFINITY)))
            ,Map.entry("mahoutsukai:scroll_boundary_gravity", new MahouSpellProfile("mahoutsukai:scroll_boundary_gravity", "boundary", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE)))
            ,Map.entry("mahoutsukai:scroll_prediction", new MahouSpellProfile("mahoutsukai:scroll_prediction", "eyes", StatType.ERUDITION, List.of(StatType.WILLPOWER)))
            ,Map.entry("mahoutsukai:scroll_black_flame", new MahouSpellProfile("mahoutsukai:scroll_black_flame", "eyes", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER)))
            ,Map.entry("mahoutsukai:scroll_treasury_projection", new MahouSpellProfile("mahoutsukai:scroll_treasury_projection", "projection", StatType.ERUDITION, List.of(StatType.MANA_POOL)))
    );

    private MahouSpellTaxonomy() {}

    public static MahouSpellProfile profile(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return PROFILES.get(MahouSpellIds.canonicalize(itemId));
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
