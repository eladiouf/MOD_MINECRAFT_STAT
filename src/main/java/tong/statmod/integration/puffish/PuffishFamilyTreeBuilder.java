package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
        String categories = Arrays.stream(StatFamily.values())
                .map(family -> "        \"" + family.slug + "\"")
                .collect(Collectors.joining(",\n"));
        return "{\n" +
                "    \"version\": 3,\n" +
                "    \"categories\": [\n" +
                categories + "\n" +
                "    ]\n" +
                "}\n";
    }

    public static String categoryId(Perk perk) {
        return "statmod:" + perk.stat.family().slug;
    }

    public static String skillId(Perk perk) {
        return perk.stat.name().toLowerCase(Locale.ROOT) + "__" + perk.name().toLowerCase(Locale.ROOT);
    }

    public static GeneratedCategoryFiles categoryFiles(StatFamily family) {
        List<StatType> stats = statsForFamily(family);
        return new GeneratedCategoryFiles(
                buildCategoryJson(family),
                buildSkillsJson(stats),
                buildDefinitionsJson(stats),
                buildConnectionsJson(stats)
        );
    }

    private static List<StatType> statsForFamily(StatFamily family) {
        return Arrays.stream(StatType.values())
                .filter(stat -> stat.family() == family)
                .toList();
    }

    private static String buildCategoryJson(StatFamily family) {
        return "{\n" +
                "    \"unlocked_by_default\": true,\n" +
                "    \"title\": \"" + escape(family.displayName) + "\",\n" +
                "    \"icon\": {\n" +
                "        \"type\": \"item\",\n" +
                "        \"data\": {\n" +
                "            \"item\": \"" + iconForFamily(family) + "\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"background\": \"" + backgroundForFamily(family) + "\"\n" +
                "}\n";
    }

    private static String buildSkillsJson(List<StatType> stats) {
        List<String> entries = new ArrayList<>();
        for (int column = 0; column < stats.size(); column++) {
            StatType stat = stats.get(column);
            for (int row = 0; row < TIER_ORDER.size(); row++) {
                Perk perk = Perk.byStatAndTier(stat, TIER_ORDER.get(row));
                if (perk == null) {
                    continue;
                }
                String id = skillId(perk);
                StringBuilder entry = new StringBuilder();
                entry.append("    \"").append(id).append("\": {\n");
                entry.append("        \"x\": ").append(column * 160).append(",\n");
                entry.append("        \"y\": ").append(row * 80).append(",\n");
                entry.append("        \"definition\": \"").append(id).append("\"");
                if (perk.tier == PerkTier.CORE) {
                    entry.append(",\n        \"root\": true");
                }
                entry.append("\n    }");
                entries.add(entry.toString());
            }
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildDefinitionsJson(List<StatType> stats) {
        List<String> entries = new ArrayList<>();
        for (StatType stat : stats) {
            for (PerkTier tier : TIER_ORDER) {
                Perk perk = Perk.byStatAndTier(stat, tier);
                if (perk == null) {
                    continue;
                }
                String id = skillId(perk);
                entries.add("    \"" + id + "\": {\n" +
                        "        \"title\": \"" + escape(perk.name) + "\",\n" +
                        "        \"description\": \"" + escape(perk.description) + "\",\n" +
                        "        \"icon\": {\n" +
                        "            \"type\": \"item\",\n" +
                        "            \"data\": {\n" +
                        "                \"item\": \"" + iconForStat(stat) + "\"\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }");
            }
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildConnectionsJson(List<StatType> stats) {
        List<String> pairs = new ArrayList<>();
        for (StatType stat : stats) {
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

    private static String iconForFamily(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> "minecraft:iron_sword";
            case RANGED_HUNT_CONTROL -> "minecraft:bow";
            case MAGICAL_CORE -> "minecraft:enchanted_book";
            case ELEMENTAL_SPECIALIZATION -> "minecraft:fire_charge";
            case MENTAL_PRESSURE_RESILIENCE -> "minecraft:totem_of_undying";
            case CRAFTING_SUPPORT -> "minecraft:anvil";
        };
    }

    private static String backgroundForFamily(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> "textures/gui/advancements/backgrounds/adventure.png";
            case RANGED_HUNT_CONTROL -> "textures/gui/advancements/backgrounds/adventure.png";
            case MAGICAL_CORE -> "textures/gui/advancements/backgrounds/end.png";
            case ELEMENTAL_SPECIALIZATION -> "textures/gui/advancements/backgrounds/nether.png";
            case MENTAL_PRESSURE_RESILIENCE -> "textures/gui/advancements/backgrounds/end.png";
            case CRAFTING_SUPPORT -> "textures/gui/advancements/backgrounds/husbandry.png";
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
}
