package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicNodeKind;
import tong.statmod.magic.MagicTier;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PuffishMagicTreeBuilder {
    public record GeneratedCategoryFiles(
            String categoryJson,
            String skillsJson,
            String definitionsJson,
            String connectionsJson
    ) {}

    private static final Map<MagicBranch, BranchLayout> BRANCH_LAYOUTS = buildBranchLayouts();
    private static final int COMMON_CENTER_X = 760;
    private static final int COMMON_TOP_Y = 200;

    private PuffishMagicTreeBuilder() {}

    public static void applyMirror(ServerPlayer player) {
        if (!ModList.get().isLoaded("puffish_skills") || !PuffishSkillsCompat.isLoaded()) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data == null) return;
        try {
            PuffishMagicSyncService.sync(data, new PuffishReflectionGateway(player));
        } catch (Throwable t) {
            STATMod.LOGGER.debug("Puffish magic mirror sync failed for {}: {}",
                    player.getName().getString(), t.getMessage());
        }
    }

    public static GeneratedCategoryFiles unifiedCategoryFiles() {
        return new GeneratedCategoryFiles(
                buildCategoryJson(),
                buildSkillsJson(),
                buildDefinitionsJson(),
                buildConnectionsJson()
        );
    }

    private static String buildCategoryJson() {
        return "{\n" +
                "    \"unlocked_by_default\": true,\n" +
                "    \"title\": \"Magic Tree\",\n" +
                "    \"icon\": {\n" +
                "        \"type\": \"item\",\n" +
                "        \"data\": {\n" +
                "            \"item\": \"minecraft:enchanted_book\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"background\": \"textures/gui/advancements/backgrounds/end.png\"\n" +
                "}\n";
    }

    private static String buildSkillsJson() {
        Map<String, NodePlacement> placements = placementsByNode();
        List<String> entries = new ArrayList<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
            NodePlacement placement = placements.get(node.id());
            if (skillId == null || placement == null) {
                continue;
            }
            StringBuilder entry = new StringBuilder();
            entry.append("    \"").append(skillId).append("\": {\n");
            entry.append("        \"x\": ").append(placement.x()).append(",\n");
            entry.append("        \"y\": ").append(placement.y()).append(",\n");
            entry.append("        \"definition\": \"").append(skillId).append("\"");
            if (isRoot(node)) {
                entry.append(",\n        \"root\": true");
            }
            entry.append("\n    }");
            entries.add(entry.toString());
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildDefinitionsJson() {
        List<String> entries = new ArrayList<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
            if (skillId == null) {
                continue;
            }
            entries.add("    \"" + skillId + "\": {\n" +
                    "        \"title\": \"" + escape(titleFor(node)) + "\",\n" +
                    "        \"description\": \"" + escape(descriptionFor(node)) + "\",\n" +
                    "        \"icon\": {\n" +
                    "            \"type\": \"item\",\n" +
                    "            \"data\": {\n" +
                    "                \"item\": \"" + iconFor(node) + "\"\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }");
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildConnectionsJson() {
        List<String> pairs = new ArrayList<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
            if (skillId == null) {
                continue;
            }
            for (String prerequisite : node.prerequisites()) {
                if (MagicTreeCatalog.LOCKED_SENTINEL.equals(prerequisite)) {
                    continue;
                }
                String prerequisiteSkillId = PuffishMagicCategoryIds.toSkillId(prerequisite);
                if (prerequisiteSkillId == null) {
                    continue;
                }
                pairs.add("            [\n" +
                        "                \"" + prerequisiteSkillId + "\",\n" +
                        "                \"" + skillId + "\"\n" +
                        "            ]");
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

    private static Map<String, NodePlacement> placementsByNode() {
        Map<String, NodePlacement> placements = new LinkedHashMap<>();

        place(placements, "common/foundation/arcane_focus", COMMON_CENTER_X, COMMON_TOP_Y);
        place(placements, "common/foundation/mana_well", COMMON_CENTER_X - 70, COMMON_TOP_Y + 140);
        place(placements, "common/foundation/cast_discipline", COMMON_CENTER_X + 70, COMMON_TOP_Y + 280);
        place(placements, "common/foundation/multi_school_gate", COMMON_CENTER_X, COMMON_TOP_Y + 430);

        for (MagicBranch branch : MagicBranch.values()) {
            if (branch == MagicBranch.COMMON) {
                continue;
            }
            placeBranch(placements, branch);
        }

        return placements;
    }

    private static void placeBranch(Map<String, NodePlacement> placements, MagicBranch branch) {
        BranchLayout layout = BRANCH_LAYOUTS.get(branch);
        if (layout == null) {
            return;
        }

        MagicNode opener = firstNode(branch, MagicNodeKind.BRANCH_OPENER);
        if (opener == null) {
            return;
        }
        place(placements, opener.id(), layout.rootX(), layout.rootY());

        List<MagicNode> tiers = nodesOf(branch, MagicNodeKind.BRANCH_TIER);
        for (int index = 0; index < tiers.size(); index++) {
            int distance = 150 + index * 160;
            place(placements, tiers.get(index).id(),
                    offsetX(layout.rootX(), layout.directionX(), distance),
                    offsetY(layout.rootY(), layout.directionY(), distance));
        }

        Map<String, List<MagicNode>> signaturesByPrereq = new LinkedHashMap<>();
        for (MagicNode node : nodesOf(branch, MagicNodeKind.SIGNATURE_SPELL)) {
            String prerequisite = node.prerequisites().isEmpty() ? opener.id() : node.prerequisites().get(0);
            signaturesByPrereq.computeIfAbsent(prerequisite, ignored -> new ArrayList<>()).add(node);
        }

        for (Map.Entry<String, List<MagicNode>> entry : signaturesByPrereq.entrySet()) {
            NodePlacement anchor = placements.get(entry.getKey());
            if (anchor == null) {
                continue;
            }
            List<MagicNode> nodes = entry.getValue();
            double center = (nodes.size() - 1) / 2.0;
            for (int index = 0; index < nodes.size(); index++) {
                double lateral = (index - center) * layout.signatureSpread();
                int x = offsetX(anchor.x(), layout.directionX(), layout.signatureForward())
                        + offsetX(0, -layout.directionY(), lateral);
                int y = offsetY(anchor.y(), layout.directionY(), layout.signatureForward())
                        + offsetY(0, layout.directionX(), lateral);
                place(placements, nodes.get(index).id(), x, y);
            }
        }
    }

    private static List<MagicNode> nodesOf(MagicBranch branch, MagicNodeKind kind) {
        List<MagicNode> nodes = new ArrayList<>();
        for (MagicNode node : MagicTreeCatalog.byBranch(branch)) {
            if (node.kind() == kind) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private static MagicNode firstNode(MagicBranch branch, MagicNodeKind kind) {
        for (MagicNode node : MagicTreeCatalog.byBranch(branch)) {
            if (node.kind() == kind) {
                return node;
            }
        }
        return null;
    }

    private static void place(Map<String, NodePlacement> placements, String nodeId, int x, int y) {
        placements.put(nodeId, new NodePlacement(x, y));
    }

    private static boolean isRoot(MagicNode node) {
        return "common/foundation/arcane_focus".equals(node.id()) || node.kind() == MagicNodeKind.BRANCH_OPENER;
    }

    private static String titleFor(MagicNode node) {
        if (node == null) {
            return "Magic";
        }
        return switch (node.id()) {
            case "common/foundation/arcane_focus" -> "Arcane Focus";
            case "common/foundation/mana_well" -> "Mana Well";
            case "common/foundation/cast_discipline" -> "Cast Discipline";
            case "common/foundation/multi_school_gate" -> "Multi-School Gate";
            default -> {
                if (node.kind() == MagicNodeKind.SIGNATURE_SPELL && !node.learnedSpells().isEmpty()) {
                    yield prettySpellName(node.learnedSpells().iterator().next());
                }
                String[] parts = node.id().split("/");
                yield titleCase(parts[parts.length - 1]);
            }
        };
    }

    private static String descriptionFor(MagicNode node) {
        if (node == null) {
            return "Magic node";
        }
        return switch (node.id()) {
            case "common/foundation/arcane_focus" -> "Anchor your first arcane discipline and awaken the tree.";
            case "common/foundation/mana_well" -> "Deepen your reserves before branching into schools.";
            case "common/foundation/cast_discipline" -> "Refine control and unlock stronger magical paths.";
            case "common/foundation/multi_school_gate" -> "Bridge multiple schools and prepare for advanced branches.";
            default -> switch (node.kind()) {
                case BRANCH_OPENER -> "Open the " + titleCase(node.branch().id) + " school.";
                case BRANCH_TIER -> "Advance through tier " + tierNumber(node.tier()) + " of the "
                        + titleCase(node.branch().id) + " path.";
                case SIGNATURE_SPELL -> "Learn " + titleFor(node) + ".";
                case TRUNK_FOUNDATION, LATEGAME_GATE -> "Progress your unified magic tree.";
            };
        };
    }

    private static String iconFor(MagicNode node) {
        if (node == null) {
            return "minecraft:enchanted_book";
        }
        return switch (node.id()) {
            case "common/foundation/arcane_focus" -> "minecraft:ender_pearl";
            case "common/foundation/mana_well" -> "minecraft:experience_bottle";
            case "common/foundation/cast_discipline" -> "minecraft:book";
            case "common/foundation/multi_school_gate" -> "minecraft:nether_star";
            default -> {
                if (node.kind() == MagicNodeKind.BRANCH_OPENER) {
                    yield openerIcon(node.branch());
                }
                if (node.kind() == MagicNodeKind.BRANCH_TIER) {
                    yield tierIcon(node.branch(), node.tier());
                }
                yield signatureIcon(node);
            }
        };
    }

    private static String openerIcon(MagicBranch branch) {
        return switch (branch) {
            case FIRE -> "minecraft:flint_and_steel";
            case WATER -> "minecraft:water_bucket";
            case AIR -> "minecraft:lightning_rod";
            case EARTH -> "minecraft:moss_block";
            case HOLY -> "minecraft:glowstone_dust";
            case BLOOD -> "minecraft:redstone";
            case ENDER -> "minecraft:ender_pearl";
            case EVOCATION -> "minecraft:totem_of_undying";
            case ELDRITCH -> "minecraft:echo_shard";
            case COMMON -> "minecraft:enchanted_book";
        };
    }

    private static String tierIcon(MagicBranch branch, MagicTier tier) {
        return switch (branch) {
            case FIRE -> switch (tier) {
                case T1 -> "minecraft:blaze_powder";
                case T2 -> "minecraft:blaze_rod";
                case T3, T4 -> "minecraft:fire_charge";
            };
            case WATER -> switch (tier) {
                case T1 -> "minecraft:snowball";
                case T2 -> "minecraft:ice";
                case T3, T4 -> "minecraft:packed_ice";
            };
            case AIR -> switch (tier) {
                case T1 -> "minecraft:feather";
                case T2 -> "minecraft:copper_bulb";
                case T3, T4 -> "minecraft:lightning_rod";
            };
            case EARTH -> switch (tier) {
                case T1 -> "minecraft:vine";
                case T2 -> "minecraft:slime_ball";
                case T3, T4 -> "minecraft:emerald";
            };
            case HOLY -> switch (tier) {
                case T1 -> "minecraft:glow_berries";
                case T2 -> "minecraft:golden_apple";
                case T3, T4 -> "minecraft:beacon";
            };
            case BLOOD -> switch (tier) {
                case T1 -> "minecraft:redstone";
                case T2 -> "minecraft:fermented_spider_eye";
                case T3, T4 -> "minecraft:wither_skeleton_skull";
            };
            case ENDER -> switch (tier) {
                case T1 -> "minecraft:chorus_fruit";
                case T2 -> "minecraft:end_crystal";
                case T3, T4 -> "minecraft:ender_eye";
            };
            case EVOCATION -> switch (tier) {
                case T1 -> "minecraft:amethyst_shard";
                case T2 -> "minecraft:goat_horn";
                case T3, T4 -> "minecraft:totem_of_undying";
            };
            case ELDRITCH -> switch (tier) {
                case T1 -> "minecraft:echo_shard";
                case T2 -> "minecraft:sculk";
                case T3, T4 -> "minecraft:sculk_catalyst";
            };
            case COMMON -> "minecraft:enchanted_book";
        };
    }

    private static String signatureIcon(MagicNode node) {
        String id = node.id();
        if (id.contains("fireball")) return "minecraft:fire_charge";
        if (id.contains("fire_breath")) return "minecraft:dragon_breath";
        if (id.contains("hellfire")) return "minecraft:lava_bucket";
        if (id.contains("burning_dash")) return "minecraft:magma_cream";
        if (id.contains("snow") || id.contains("frost") || id.contains("ice") || id.contains("blizzard")) return "minecraft:snowball";
        if (id.contains("lightning") || id.contains("thunder") || id.contains("shock") || id.contains("volt")) return "minecraft:lightning_rod";
        if (id.contains("poison") || id.contains("acid") || id.contains("root") || id.contains("oakskin")) return "minecraft:spider_eye";
        if (id.contains("heal") || id.contains("regeneration") || id.contains("blessing") || id.contains("sunbeam")) return "minecraft:golden_apple";
        if (id.contains("blood") || id.contains("siphon") || id.contains("sacrifice") || id.contains("wither")) return "minecraft:redstone";
        if (id.contains("portal") || id.contains("teleport") || id.contains("ender") || id.contains("black_hole")) return "minecraft:ender_pearl";
        if (id.contains("vex") || id.contains("fang") || id.contains("wololo") || id.contains("creeper")) return "minecraft:totem_of_undying";
        if (id.contains("eldritch") || id.contains("sculk") || id.contains("abyss") || id.contains("dimension")) return "minecraft:echo_shard";
        if (id.contains("tensura")) return branchSignatureFallback(node.branch());
        return branchSignatureFallback(node.branch());
    }

    private static String branchSignatureFallback(MagicBranch branch) {
        return switch (branch) {
            case FIRE -> "minecraft:blaze_powder";
            case WATER -> "minecraft:prismarine_crystals";
            case AIR -> "minecraft:feather";
            case EARTH -> "minecraft:moss_block";
            case HOLY -> "minecraft:glowstone_dust";
            case BLOOD -> "minecraft:redstone";
            case ENDER -> "minecraft:ender_eye";
            case EVOCATION -> "minecraft:amethyst_shard";
            case ELDRITCH -> "minecraft:echo_shard";
            case COMMON -> "minecraft:enchanted_book";
        };
    }

    private static String prettySpellName(String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return "Spell";
        }
        String raw = spellId;
        int namespaceSeparator = raw.indexOf(':');
        String namespace = namespaceSeparator >= 0 ? raw.substring(0, namespaceSeparator) : "";
        String path = namespaceSeparator >= 0 ? raw.substring(namespaceSeparator + 1) : raw;
        String title = titleCase(path);
        if ("tensura".equals(namespace)) {
            return "Tensura " + title;
        }
        return title;
    }

    private static String titleCase(String raw) {
        String[] words = raw.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.length() == 0 ? raw : builder.toString();
    }

    private static int tierNumber(MagicTier tier) {
        return switch (tier) {
            case T1 -> 1;
            case T2 -> 2;
            case T3 -> 3;
            case T4 -> 4;
        };
    }

    private static int offsetX(int originX, double directionX, double distance) {
        return originX + (int) Math.round(directionX * distance);
    }

    private static int offsetY(int originY, double directionY, double distance) {
        return originY + (int) Math.round(directionY * distance);
    }

    private static String escape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static Map<MagicBranch, BranchLayout> buildBranchLayouts() {
        Map<MagicBranch, BranchLayout> layouts = new EnumMap<>(MagicBranch.class);
        layouts.put(MagicBranch.FIRE, new BranchLayout(500, 260, -0.83, -0.56, 125, 95));
        layouts.put(MagicBranch.WATER, new BranchLayout(1020, 260, 0.83, -0.56, 125, 95));
        layouts.put(MagicBranch.AIR, new BranchLayout(360, 610, -0.98, 0.20, 115, 88));
        layouts.put(MagicBranch.EARTH, new BranchLayout(1160, 610, 0.98, 0.20, 115, 88));
        layouts.put(MagicBranch.HOLY, new BranchLayout(280, 980, -0.76, 0.65, 110, 90));
        layouts.put(MagicBranch.BLOOD, new BranchLayout(540, 1120, -0.34, 0.94, 105, 84));
        layouts.put(MagicBranch.ENDER, new BranchLayout(760, 1180, 0.0, 1.0, 110, 86));
        layouts.put(MagicBranch.EVOCATION, new BranchLayout(980, 1120, 0.34, 0.94, 105, 84));
        layouts.put(MagicBranch.ELDRITCH, new BranchLayout(1240, 980, 0.76, 0.65, 110, 90));
        return layouts;
    }

    private record BranchLayout(
            int rootX,
            int rootY,
            double directionX,
            double directionY,
            int signatureForward,
            int signatureSpread
    ) {}

    private record NodePlacement(int x, int y) {}
}
