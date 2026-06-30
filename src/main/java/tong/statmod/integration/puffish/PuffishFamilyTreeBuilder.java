package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PuffishFamilyTreeBuilder {
    public record GeneratedCategoryFiles(
            String categoryJson,
            String skillsJson,
            String definitionsJson,
            String connectionsJson
    ) {}

    private static final List<PerkTier> TIER_ORDER = List.of(
            PerkTier.CORE,
            PerkTier.ACTIVE,
            PerkTier.SYNERGY,
            PerkTier.SITUATIONAL,
            PerkTier.MASTERY,
            PerkTier.TRANSCENDENCE
    );
    private PuffishFamilyTreeBuilder() {}

    public static String configJson() {
        return "{\n" +
                "    \"version\": 3,\n" +
                "    \"categories\": [\n" +
                "        \"statmod_perks\",\n" +
                "        \"statmod_magic\"\n" +
                "    ]\n" +
                "}\n";
    }

    public static String categoryId(Perk perk) {
        return PuffishPerkIds.UNIFIED_CATEGORY;
    }

    public static String skillId(Perk perk) {
        return perk.stat.name().toLowerCase(Locale.ROOT) + "__" + perk.name().toLowerCase(Locale.ROOT);
    }

    public static GeneratedCategoryFiles unifiedCategoryFiles() {
        return new GeneratedCategoryFiles(
                buildUnifiedCategoryJson(),
                buildUnifiedSkillsJson(),
                buildUnifiedDefinitionsJson(),
                buildUnifiedConnectionsJson()
        );
    }

    private static String buildUnifiedCategoryJson() {
        return "{\n" +
                "    \"unlocked_by_default\": true,\n" +
                "    \"title\": \"Perk Tree\",\n" +
                "    \"icon\": {\n" +
                "        \"type\": \"item\",\n" +
                "        \"data\": {\n" +
                "            \"item\": \"minecraft:nether_star\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"background\": \"textures/block/deepslate_tiles.png\",\n" +
                "    \"colors\": {\n" +
                "        \"connections\": {\n" +
                "            \"locked\":     { \"stroke\": \"#3A3F46FF\", \"fill\": \"#0E1014FF\" },\n" +
                "            \"affordable\": { \"stroke\": \"#D4FF0099\", \"fill\": \"#0E1014FF\" },\n" +
                "            \"unlocked\":   { \"stroke\": \"#D4FF00FF\", \"fill\": \"#0E1014FF\" },\n" +
                "            \"excluded\":   { \"stroke\": \"#5A1E1EFF\", \"fill\": \"#0E1014FF\" }\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private static String buildUnifiedSkillsJson() {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<StatFamily, List<StatType>> familyEntry : familyStats().entrySet()) {
            FamilyLayout layout = layoutForFamily(familyEntry.getKey());
            List<StatType> stats = familyEntry.getValue();
            for (int statIndex = 0; statIndex < stats.size(); statIndex++) {
                StatType stat = stats.get(statIndex);
                for (int row = 0; row < TIER_ORDER.size(); row++) {
                    int x = layout.rootX() + statIndex * layout.laneDx() + row * layout.tierDx();
                    int y = layout.rootY() + statIndex * layout.laneDy() + row * layout.tierDy();
                    Perk perk = Perk.byStatAndTier(stat, TIER_ORDER.get(row));
                    if (perk == null) {
                        continue;
                    }
                    String id = skillId(perk);
                    StringBuilder entry = new StringBuilder();
                    entry.append("    \"").append(id).append("\": {\n");
                    entry.append("        \"x\": ").append(x).append(",\n");
                    entry.append("        \"y\": ").append(y).append(",\n");
                    entry.append("        \"definition\": \"").append(id).append("\"");
                    if (perk.tier == PerkTier.CORE) {
                        entry.append(",\n        \"root\": true");
                    }
                    entry.append("\n    }");
                    entries.add(entry.toString());
                }
            }
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildUnifiedDefinitionsJson() {
        List<String> entries = new ArrayList<>();
        for (Perk perk : Perk.values()) {
            String id = skillId(perk);
            entries.add("    \"" + id + "\": {\n" +
                    "        \"title\": \"" + escape(perk.name) + "\",\n" +
                    "        \"description\": \"" + escape(perk.description) + "\",\n" +
                    "        \"icon\": {\n" +
                    "            \"type\": \"item\",\n" +
                    "            \"data\": {\n" +
                    "                \"item\": \"" + iconForStat(perk.stat) + "\"\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }");
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildUnifiedConnectionsJson() {
        List<String> pairs = new ArrayList<>();
        for (StatType stat : StatType.values()) {
            Perk previous = null;
            for (PerkTier tier : TIER_ORDER) {
                Perk perk = Perk.byStatAndTier(stat, tier);
                if (perk == null) {
                    continue;
                }
                if (previous != null) {
                    pairs.add("            [\n" +
                            "                \"" + skillId(previous) + "\",\n" +
                            "                \"" + skillId(perk) + "\"\n" +
                            "            ]");
                }
                previous = perk;
            }
        }
        return "{\n" +
                "    \"normal\": {\n" +
                "        \"bidirectional\": [\n" +
                String.join(",\n", pairs) + "\n" +
                "        ]\n" +
                "    }\n" +
                "}\n";
    }

    private static Map<StatFamily, List<StatType>> familyStats() {
        Map<StatFamily, List<StatType>> byFamily = new LinkedHashMap<>();
        for (StatFamily family : StatFamily.values()) {
            byFamily.put(family, Arrays.stream(StatType.values())
                    .filter(stat -> stat.family() == family)
                    .toList());
        }
        return byFamily;
    }

    private static FamilyLayout layoutForFamily(StatFamily family) {
        // Origin (0,0) = visual center. Each family is a spoke radiating outward from the
        // center. Core (root) sits near the center, tiers move outward, lanes fan out
        // perpendicular to the spoke direction. Compact spacing pour tenir dans une vue
        // par défaut Puffish (scale 1.0, fenêtre ~480×260 visible).
        return switch (family) {
            // North-west spoke — 6 stats fanning vertically, tiers going up-left
            case FRONTLINE_PHYSICAL_COMBAT -> new FamilyLayout(-90, -40, -40, 38, -68, -42);
            // North-east spoke — 3 stats fanning vertically, tiers going up-right
            case RANGED_HUNT_CONTROL      -> new FamilyLayout( 90, -40,  40, 52,  68, -42);
            // North spoke — 5 stats fanning horizontally, tiers going up
            case MAGICAL_CORE             -> new FamilyLayout(-110, -80, 55,  0,   0, -58);
            // South spoke — 4 stats fanning horizontally, tiers going down
            case ELEMENTAL_SPECIALIZATION -> new FamilyLayout( -80, 130, 55,  0,   0,  58);
            // South-west spoke — 2 stats fanning vertically, tiers going down-left
            case MENTAL_PRESSURE_RESILIENCE -> new FamilyLayout(-100, 110, -50, 64, -64, 40);
            // South-east spoke — 3 stats fanning vertically, tiers going down-right
            case CRAFTING_SUPPORT         -> new FamilyLayout( 100, 110,  50, 64,  64, 40);
        };
    }

    private static String iconForStat(StatType stat) {
        return switch (stat) {
            case BRUTE_FORCE -> "minecraft:iron_axe";
            case BLADE_TECHNIQUE -> "minecraft:iron_sword";
            case RAPIDITE -> "minecraft:golden_sword";
            case AGILITY -> "minecraft:feather";
            case PHYSICAL_RESISTANCE -> "minecraft:shield";
            case PHYSICAL_ENDURANCE -> "minecraft:cooked_beef";
            case PRECISION -> "minecraft:bow";
            case TRACKING -> "minecraft:spyglass";
            case KEEN_SENSES -> "minecraft:ender_eye";
            case ARCANE_POWER -> "minecraft:blaze_rod";
            case WATER_AFFINITY -> "minecraft:water_bucket";
            case EARTH_AFFINITY -> "minecraft:stone";
            case FIRE_AFFINITY -> "minecraft:fire_charge";
            case AIR_AFFINITY -> "minecraft:feather";
            case MAGIC_RESISTANCE -> "minecraft:shield";
            case CASTING_SPEED -> "minecraft:clock";
            case MANA_POOL -> "minecraft:amethyst_shard";
            case ERUDITION -> "minecraft:book";
            case FORGING -> "minecraft:anvil";
            case COOKING -> "minecraft:cooked_beef";
            case ALCHEMY -> "minecraft:brewing_stand";
            case INTIMIDATION -> "minecraft:wither_skeleton_skull";
            case WILLPOWER -> "minecraft:totem_of_undying";
        };
    }

    private static String escape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private record FamilyLayout(
            int rootX,
            int rootY,
            int laneDx,
            int laneDy,
            int tierDx,
            int tierDy
    ) {}
}
