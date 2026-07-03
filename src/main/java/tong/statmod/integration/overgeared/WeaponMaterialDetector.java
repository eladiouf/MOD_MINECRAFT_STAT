package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Mission M5 — Phase γ.
 *
 * <p>Détecte le matériau implicite d'une arme à partir de son id (path). Logique
 * volontairement identique à celle du script Python {@code tools/generate_assembly_recipes.py}
 * pour que le runtime puisse appliquer le gating au craft sur la même base que la
 * génération de recettes.
 *
 * <p>L'ordre des préfixes matters — plus spécifique d'abord
 * ({@code high_magisteel} avant {@code magisteel}).
 */
public final class WeaponMaterialDetector {

    private static final List<Map.Entry<String, String>> PREFIXES = buildPrefixes();

    /**
     * Retourne le matériau détecté pour {@code weaponId} (par exemple "iron" pour
     * "simplyswords:iron_cutlass") ou null si aucun préfixe ne matche.
     */
    public static String detectMaterial(ResourceLocation weaponId) {
        if (weaponId == null) return null;
        String path = weaponId.getPath();
        if (path == null || path.isEmpty()) return null;
        for (Map.Entry<String, String> entry : PREFIXES) {
            String prefix = entry.getKey();
            if (path.startsWith(prefix + "_") || path.equals(prefix)) {
                return entry.getValue();
            }
            // Aussi si le préfixe apparaît n'importe où séparé par _
            if (path.contains("_" + prefix + "_")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static List<Map.Entry<String, String>> buildPrefixes() {
        return List.of(
                // Tier matériau canonique — ordre spécifique → générique
                Map.entry("hihiirokane",     "hihiirokane"),
                Map.entry("high_magisteel",  "high_magisteel"),
                Map.entry("pure_magisteel",  "pure_magisteel"),
                Map.entry("low_magisteel",   "low_magisteel"),
                Map.entry("magisteel",       "magisteel"),
                Map.entry("adamantite",      "adamantite"),
                Map.entry("orichalcum",      "orichalcum"),
                Map.entry("mithril",         "mithril"),
                Map.entry("arcane",          "arcane"),
                Map.entry("pyrium",          "pyrium"),
                Map.entry("bronze",          "bronze"),
                Map.entry("netherite",       "netherite"),
                Map.entry("diamond",         "diamond"),
                Map.entry("steel",           "steel"),
                Map.entry("silver",          "silver"),
                Map.entry("golden",          "gold"),
                Map.entry("gold",            "gold"),
                Map.entry("iron",            "iron"),
                Map.entry("copper",          "copper"),
                Map.entry("tin",             "tin"),
                Map.entry("stone",           "stone"),
                Map.entry("wooden",          "wood"),
                Map.entry("wood",            "wood"),
                // Alias thématiques
                Map.entry("runic",           "arcane"),
                Map.entry("netherfused",     "high_magisteel"),
                Map.entry("runefused",       "arcane"),
                Map.entry("soulkeeper",      "high_magisteel"),
                Map.entry("brimstone",       "pure_magisteel"),
                Map.entry("watcher",         "adamantite"),
                Map.entry("fiery",           "magisteel"),
                Map.entry("soul",            "high_magisteel"),
                Map.entry("wraith",          "adamantite"),
                Map.entry("frostbound",      "mithril"),
                Map.entry("frost",           "mithril"),
                Map.entry("ash",             "magisteel"),
                Map.entry("ashen",           "magisteel"),
                Map.entry("ancient",         "orichalcum"),
                Map.entry("legendary",       "hihiirokane"),
                Map.entry("priest",          "arcane"),
                Map.entry("paladin",         "mithril"),
                Map.entry("infernal",        "pyrium"),
                Map.entry("knight",          "steel"),
                Map.entry("samurai",         "steel")
        );
    }

    private WeaponMaterialDetector() {}
}
