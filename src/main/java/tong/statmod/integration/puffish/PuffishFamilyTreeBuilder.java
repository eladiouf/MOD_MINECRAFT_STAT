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
    private static final int VIEWPORT_CENTER_X = 980;
    private static final int VIEWPORT_CENTER_Y = 760;
    private static final int VIEWPORT_MARGIN_X = 40;
    private static final int VIEWPORT_MARGIN_Y = 40;
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
        Map<String, NodePlacement> placements = placementsBySkillId();
        List<String> entries = new ArrayList<>();
        for (Perk perk : Perk.values()) {
            String id = skillId(perk);
            NodePlacement placement = placements.get(id);
            if (placement == null) {
                continue;
            }
            StringBuilder entry = new StringBuilder();
            entry.append("    \"").append(id).append("\": {\n");
            entry.append("        \"x\": ").append(placement.x()).append(",\n");
            entry.append("        \"y\": ").append(placement.y()).append(",\n");
            entry.append("        \"definition\": \"").append(id).append("\"");
            if (perk.tier == PerkTier.CORE) {
                entry.append(",\n        \"root\": true");
            }
            entry.append("\n    }");
            entries.add(entry.toString());
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

    private static Map<String, NodePlacement> placementsBySkillId() {
        Map<String, NodePlacement> placements = new LinkedHashMap<>();
        for (Map.Entry<StatFamily, List<StatType>> familyEntry : familyStats().entrySet()) {
            FamilyLayout layout = layoutForFamily(familyEntry.getKey());
            placeFamily(placements, layout, familyEntry.getValue());
        }
        recenterOnViewport(placements);
        return placements;
    }

    private static void placeFamily(Map<String, NodePlacement> placements, FamilyLayout layout, List<StatType> stats) {
        for (int statIndex = 0; statIndex < stats.size(); statIndex++) {
            StatType stat = stats.get(statIndex);
            double statAngle = angleForStat(layout, statIndex, stats.size());

            for (int tierIndex = 0; tierIndex < TIER_ORDER.size(); tierIndex++) {
                Perk perk = Perk.byStatAndTier(stat, TIER_ORDER.get(tierIndex));
                if (perk == null) {
                    continue;
                }
                placements.put(skillId(perk), polarPlacement(
                        statAngle,
                        layout.rootRadius() + tierIndex * layout.tierStep()
                ));
            }
        }
    }

    private static double angleForStat(FamilyLayout layout, int statIndex, int statCount) {
        if (statCount <= 1) {
            return layout.centerAngleDegrees();
        }
        double angleStep = layout.spreadDegrees() / (statCount - 1.0);
        return layout.centerAngleDegrees() - layout.spreadDegrees() / 2.0 + statIndex * angleStep;
    }

    private static NodePlacement polarPlacement(double angleDegrees, int radius) {
        double angleRadians = Math.toRadians(angleDegrees);
        int x = (int) Math.round(Math.cos(angleRadians) * radius);
        int y = (int) Math.round(Math.sin(angleRadians) * radius);
        return new NodePlacement(x, y);
    }

    private static void recenterOnViewport(Map<String, NodePlacement> placements) {
        Bounds bounds = boundsOf(placements);
        int currentCenterX = (bounds.minX() + bounds.maxX()) / 2;
        int currentCenterY = (bounds.minY() + bounds.maxY()) / 2;
        int shiftX = VIEWPORT_CENTER_X - currentCenterX;
        int shiftY = VIEWPORT_CENTER_Y - currentCenterY;

        if (bounds.minX() + shiftX < VIEWPORT_MARGIN_X) {
            shiftX += VIEWPORT_MARGIN_X - (bounds.minX() + shiftX);
        }
        if (bounds.minY() + shiftY < VIEWPORT_MARGIN_Y) {
            shiftY += VIEWPORT_MARGIN_Y - (bounds.minY() + shiftY);
        }

        for (Map.Entry<String, NodePlacement> entry : placements.entrySet()) {
            NodePlacement placement = entry.getValue();
            entry.setValue(new NodePlacement(placement.x() + shiftX, placement.y() + shiftY));
        }
    }

    private static Bounds boundsOf(Map<String, NodePlacement> placements) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (NodePlacement placement : placements.values()) {
            minX = Math.min(minX, placement.x());
            maxX = Math.max(maxX, placement.x());
            minY = Math.min(minY, placement.y());
            maxY = Math.max(maxY, placement.y());
        }
        return new Bounds(minX, maxX, minY, maxY);
    }

    private static FamilyLayout layoutForFamily(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> new FamilyLayout(208.0, 66.0, 350, 108);
            case RANGED_HUNT_CONTROL -> new FamilyLayout(335.0, 30.0, 335, 108);
            case MAGICAL_CORE -> new FamilyLayout(280.0, 52.0, 330, 108);
            case ELEMENTAL_SPECIALIZATION -> new FamilyLayout(90.0, 48.0, 340, 108);
            case MENTAL_PRESSURE_RESILIENCE -> new FamilyLayout(150.0, 24.0, 300, 108);
            case CRAFTING_SUPPORT -> new FamilyLayout(30.0, 36.0, 335, 108);
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
            double centerAngleDegrees,
            double spreadDegrees,
            int rootRadius,
            int tierStep
    ) {}

    private record NodePlacement(int x, int y) {}

    private record Bounds(int minX, int maxX, int minY, int maxY) {}
}
