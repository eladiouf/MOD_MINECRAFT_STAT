package tong.statmod.integration.mahou;

import java.util.Locale;
import java.util.Map;

public final class MahouSpellIds {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("mahoutsukai:gandr_spell_scroll", "mahoutsukai:scroll_gandr"),
            Map.entry("mahoutsukai:rho_aias_spell_scroll", "mahoutsukai:scroll_rho_aias"),
            Map.entry("mahoutsukai:fallen_down_spell_scroll", "mahoutsukai:scroll_fallen_down"),
            Map.entry("mahoutsukai:mystic_staff_spell_scroll", "mahoutsukai:scroll_mystic_staff")
    );

    private MahouSpellIds() {}

    public static String canonicalize(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return itemId;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(normalized, normalized);
    }
}
