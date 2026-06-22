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
    private static final List<String> MAGIC_CATEGORY_SLUGS = List.of(
            "statmod_magic"
    );

    private PuffishFamilyTreeBuilder() {}

    public static String configJson() {
        List<String> categories = new ArrayList<>();
        categories.add("        \"statmod_perks\"");
        for (String magicCategory : MAGIC_CATEGORY_SLUGS) {
            categories.add("        \"" + magicCategory + "\"");
        }
        return "{\n" +
                "    \"version\": 3,\n" +
                "    \"categories\": [\n" +
                String.join(",\n", categories) + "\n" +
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
                "    \"background\": \"textures/gui/advancements/backgrounds/adventure.png\"\n" +
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
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> new FamilyLayout(780, 430, -55, 52, -95, -60);
            case RANGED_HUNT_CONTROL -> new FamilyLayout(930, 430, 60, 74, 95, -58);
            case MAGICAL_CORE -> new FamilyLayout(560, 480, 95, 0, 0, -82);
            case ELEMENTAL_SPECIALIZATION -> new FamilyLayout(610, 720, 100, 0, 0, 82);
            case MENTAL_PRESSURE_RESILIENCE -> new FamilyLayout(620, 720, -70, 90, -90, 55);
            case CRAFTING_SUPPORT -> new FamilyLayout(980, 720, 75, 90, 90, 55);
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
