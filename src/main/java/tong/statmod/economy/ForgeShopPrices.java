package tong.statmod.economy;

import tong.statmod.item.ForgingIntermediateIds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Prix des items de forge en FDP_cfa.
 *
 * <p>Les roughs (rough_{class}_{material}) sont tarifés par tier de matériau.
 * Les blueprints, grips, components et tools ont des prix explicites.
 */
public final class ForgeShopPrices {

    private static final int[] TIER_PRICES = {
            50,   // 0: gold
            50,   // 1: tin
            70,   // 2: bronze
            100,  // 3: diamond
            150,  // 4: pyrium
            250,  // 5: arcane
            400,  // 6: mithril
            500,  // 7: low_magisteel
            600,  // 8: magisteel
            700,  // 9: pure_magisteel
            800,  // 10: high_magisteel
            1000, // 11: orichalcum
            1500, // 12: adamantite
            3000, // 13: hihiirokane
            3500, // 14: netherite
    };

    private static final Map<String, Integer> MATERIAL_TIER = buildTierMap();

    private static final Map<String, Long> OVERRIDES = buildOverrides();

    private ForgeShopPrices() {}

    /**
     * Retourne le prix en FDP_cfa de l'item identifié par son itemId (path du registry).
     */
    public static OptionalLong priceOf(String itemId) {
        Long ov = OVERRIDES.get(itemId);
        if (ov != null) return OptionalLong.of(ov);

        // Extract material from rough_{class}_{material}
        Integer tier = materialTierFromRoughId(itemId);
        if (tier != null) return OptionalLong.of(TIER_PRICES[tier]);

        return OptionalLong.empty();
    }

    /**
     * Parcourt les matériaux connus (du plus long au plus court) et vérifie si
     * {@code id} se termine par {@code _<material>}.
     */
    private static Integer materialTierFromRoughId(String id) {
        if (id == null || !id.startsWith("rough_")) return null;
        for (String mat : MATERIAL_TIER.keySet()) {
            if (id.endsWith("_" + mat)) return MATERIAL_TIER.get(mat);
        }
        return null;
    }

    /**
     * Construit la map matériau → index de tier à partir de {@link
     * ForgingIntermediateIds#MATERIALS}.
     */
    private static Map<String, Integer> buildTierMap() {
        List<String> materials = ForgingIntermediateIds.MATERIALS;
        Map<String, Integer> m = new HashMap<>(materials.size());
        // Sort by length descending so "low_magisteel" matches before "magisteel"
        for (int i = 0; i < materials.size(); i++) {
            m.put(materials.get(i), i);
        }
        return Map.copyOf(m);
    }

    private static Map<String, Long> buildOverrides() {
        Map<String, Long> m = new HashMap<>();

        // Blueprints
        m.put("blueprint_universal_blade", 500L);
        m.put("blueprint_universal_pole", 800L);
        m.put("blueprint_runic_blade", 3000L);
        m.put("blueprint_legendary", 10000L);

        // Grips
        m.put("wooden_grip", 200L);
        m.put("leather_wrap", 400L);
        m.put("wire_wrap", 350L);
        m.put("runic_grip", 1200L);

        // Form components
        m.put("katana_tsuba", 250L);
        m.put("rapier_guard", 200L);
        m.put("claymore_pommel", 300L);
        m.put("halberd_socket", 350L);
        m.put("warhammer_core", 400L);
        m.put("staff_focus", 500L);

        // Tools
        m.put("basic_forge_tongs", 400L);
        m.put("basic_smithing_hammer", 500L);

        return Map.copyOf(m);
    }
}
