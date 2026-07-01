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
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.ZipFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public final class PuffishMagicTreeBuilder {
    public record GeneratedCategoryFiles(
            String categoryJson,
            String skillsJson,
            String definitionsJson,
            String connectionsJson
    ) {}

    private static final Map<MagicBranch, BranchLayout> BRANCH_LAYOUTS = buildBranchLayouts();
    private static final int CANVAS_OFFSET_X = 230;
    private static final int CANVAS_OFFSET_Y = 240;
    private static final int CONSTELLATION_CENTER_X = 760;
    private static final int CONSTELLATION_CENTER_Y = 700;
    private static final int VIEWPORT_CENTER_X = 980;
    private static final int VIEWPORT_CENTER_Y = 760;
    private static final int VIEWPORT_MIN_MARGIN_X = 32;
    private static final int VIEWPORT_MIN_MARGIN_Y = 32;
    private static final double MIN_NODE_SPACING = 52.0;
    private static final int RELAX_ITERATIONS = 400;
    private static final double RELAX_SPRING = 0.06;
    private static final double RELAX_MAX_STEP = 20.0;
    private static final int ROOT_HALO_RADIUS = 260;
    private static final int OPENER_RADIUS = 280;
    private static final int TIER_RADIUS_STEP = 140;
    private static final int SIGNATURE_FORWARD_OFFSET = 150;
    private static final int SIGNATURE_ROW_DEPTH = 40;
    private static final int SIGNATURE_SIDE_OFFSET = 52;
    private static final int MAX_SIGNATURES_PER_ROW = 3;
    private static final Path LIBS_DIR = Path.of("libs");
    private static final Path RUN_MODS_DIR = Path.of("runs", "client", "mods");
    private static final Map<String, String> DISCOVERED_SPELL_TEXTURES = discoverSpellTextures();

    private PuffishMagicTreeBuilder() {}

    public static void applyMirror(ServerPlayer player) {
        if (!ModList.get().isLoaded("puffish_skills") || !PuffishSkillsCompat.isLoaded()) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data == null) return;
        PuffishSkillsCompat.runGuarded(player, () -> {
            try {
                PuffishMagicSyncService.sync(data, new PuffishReflectionGateway(player));
            } catch (Throwable t) {
                STATMod.LOGGER.debug("Puffish magic mirror sync failed for {}: {}",
                        player.getName().getString(), t.getMessage());
            }
        });
    }

    public static GeneratedCategoryFiles unifiedCategoryFiles() {
        return new GeneratedCategoryFiles(
                buildCategoryJson(),
                buildSkillsJson(),
                buildDefinitionsJson(),
                buildConnectionsJson()
        );
    }

    static LayoutSnapshot debugLayoutSnapshot() {
        Map<String, NodePlacement> placements = placementsByNode();
        Bounds bounds = boundsOf(placements);
        NodePlacement root = placements.get("common/foundation/arcane_focus");
        NodePlacement boundsCenter = new NodePlacement(
                (bounds.minX() + bounds.maxX()) / 2,
                (bounds.minY() + bounds.maxY()) / 2
        );
        return new LayoutSnapshot(Map.copyOf(placements), bounds, root, boundsCenter);
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
                "    \"background\": \"minecraft:textures/block/deepslate_bricks.png\",\n" +
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

    private static String buildSkillsJson() {
        Map<String, NodePlacement> placements = placementsByNode();
        List<String> entries = new ArrayList<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            if (!isTreeVisible(node)) {
                continue;
            }
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

    private static boolean isTreeVisible(MagicNode node) {
        return switch (node.id()) {
            case "common/foundation/mana_well",
                 "common/foundation/cast_discipline",
                 "common/foundation/multi_school_gate" -> false;
            default -> true;
        };
    }

    private static String buildDefinitionsJson() {
        List<String> entries = new ArrayList<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            if (!isTreeVisible(node)) {
                continue;
            }
            String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
            if (skillId == null) {
                continue;
            }
            IconSpec icon = iconFor(node);
            entries.add("    \"" + skillId + "\": {\n" +
                    "        \"title\": \"" + escape(titleFor(node)) + "\",\n" +
                    "        \"description\": \"" + escape(descriptionFor(node)) + "\",\n" +
                    icon.jsonBlock() + ",\n" +
                    "        \"size\": " + formatSize(sizeFor(node)) + "\n" +
                    "    }");
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildConnectionsJson() {
        List<String> pairs = new ArrayList<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            if (!isTreeVisible(node)) {
                continue;
            }
            String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
            if (skillId == null) {
                continue;
            }
            String visualParent = visualParentId(node);
            if (visualParent == null || MagicTreeCatalog.LOCKED_SENTINEL.equals(visualParent)) {
                continue;
            }
            String prerequisiteSkillId = PuffishMagicCategoryIds.toSkillId(visualParent);
            if (prerequisiteSkillId == null) {
                continue;
            }
            pairs.add("            [\n" +
                    "                \"" + prerequisiteSkillId + "\",\n" +
                    "                \"" + skillId + "\"\n" +
                    "            ]");
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

        place(placements, "common/foundation/arcane_focus", CONSTELLATION_CENTER_X, CONSTELLATION_CENTER_Y);

        for (MagicBranch branch : MagicBranch.values()) {
            if (branch == MagicBranch.COMMON) {
                continue;
            }
            placeBranch(placements, branch);
        }

        relaxSignaturePlacements(placements);
        enforceRootHalo(placements, "common/foundation/arcane_focus", ROOT_HALO_RADIUS);
        recenterOnVisualCenter(placements, new NodePlacement(VIEWPORT_CENTER_X, VIEWPORT_CENTER_Y));
        return placements;
    }

    static Bounds boundsOf(Map<String, NodePlacement> placements) {
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

    static double nearestDistanceFrom(String nodeId, Map<String, NodePlacement> placements) {
        NodePlacement origin = placements.get(nodeId);
        if (origin == null) {
            return Double.POSITIVE_INFINITY;
        }

        double nearest = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, NodePlacement> entry : placements.entrySet()) {
            if (nodeId.equals(entry.getKey())) {
                continue;
            }
            NodePlacement placement = entry.getValue();
            int dx = origin.x() - placement.x();
            int dy = origin.y() - placement.y();
            double distance = Math.sqrt(dx * dx + dy * dy);
            nearest = Math.min(nearest, distance);
        }
        return nearest;
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
        int openerRadius = layout.openerRadius();
        placePolar(placements, opener.id(), layout.angleDegrees(), openerRadius);

        List<MagicNode> tiers = nodesOf(branch, MagicNodeKind.BRANCH_TIER);
        for (int index = 0; index < tiers.size(); index++) {
            int radius = openerRadius + (index + 1) * layout.tierRadiusStep();
            placePolar(placements, tiers.get(index).id(), layout.angleDegrees(), radius);
        }

        Map<String, List<MagicNode>> signaturesByPrereq = new LinkedHashMap<>();
        for (MagicNode node : nodesOf(branch, MagicNodeKind.SIGNATURE_SPELL)) {
            String prerequisite = node.prerequisites().isEmpty() ? opener.id() : node.prerequisites().get(0);
            signaturesByPrereq.computeIfAbsent(prerequisite, ignored -> new ArrayList<>()).add(node);
        }

        int clusterIndex = 0;
        int clusterCount = signaturesByPrereq.size();
        for (Map.Entry<String, List<MagicNode>> entry : signaturesByPrereq.entrySet()) {
            NodePlacement anchor = placements.get(entry.getKey());
            if (anchor == null) {
                continue;
            }
            placeSignatureCluster(placements, layout, anchor, entry.getValue(), clusterIndex++, clusterCount);
        }
    }

    private static void placeSignatureCluster(Map<String, NodePlacement> placements,
                                              BranchLayout layout,
                                              NodePlacement anchor,
                                              List<MagicNode> nodes,
                                              int clusterIndex,
                                              int clusterCount) {
        int localAnchorX = anchor.x() - CANVAS_OFFSET_X;
        int localAnchorY = anchor.y() - CANVAS_OFFSET_Y;
        double clusterOrdinal = clusterCount <= 1 ? 0.0 : clusterIndex - (clusterCount - 1) / 2.0;
        double clusterAngle = layout.angleDegrees() + clusterOrdinal * layout.clusterFanDegrees();
        double radialX = Math.cos(Math.toRadians(clusterAngle));
        double radialY = Math.sin(Math.toRadians(clusterAngle));
        double tangentX = -radialY;
        double tangentY = radialX;
        double sideShift = layout.signatureSideBias() * layout.signatureSideOffset()
                + clusterOrdinal * layout.clusterTangentSeparation();
        double baseX = localAnchorX + tangentX * sideShift;
        double baseY = localAnchorY + tangentY * sideShift;

        List<Integer> rowSizes = balancedRowSizes(nodes.size());
        int index = 0;
        for (int row = 0; row < rowSizes.size(); row++) {
            int rowSize = rowSizes.get(row);
            double forward = layout.signatureForwardOffset() + row * layout.signatureRowDepth();
            for (int col = 0; col < rowSize; col++) {
                double lane = (col - (rowSize - 1) / 2.0) * layout.signatureLaneStep();
                int x = (int) Math.round(baseX + radialX * forward + tangentX * lane);
                int y = (int) Math.round(baseY + radialY * forward + tangentY * lane);
                place(placements, nodes.get(index++).id(), x, y);
            }
        }
    }

    private static List<Integer> balancedRowSizes(int total) {
        int rows = Math.max(1, (int) Math.ceil(total / (double) MAX_SIGNATURES_PER_ROW));
        int base = total / rows;
        int remainder = total % rows;
        List<Integer> sizes = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            sizes.add(base + (row < remainder ? 1 : 0));
        }
        return sizes;
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
        placements.put(nodeId, new NodePlacement(x + CANVAS_OFFSET_X, y + CANVAS_OFFSET_Y));
    }

    private static void placePolar(Map<String, NodePlacement> placements, String nodeId, double angleDegrees, int radius) {
        double radians = Math.toRadians(angleDegrees);
        int x = CONSTELLATION_CENTER_X + (int) Math.round(Math.cos(radians) * radius);
        int y = CONSTELLATION_CENTER_Y + (int) Math.round(Math.sin(radians) * radius);
        place(placements, nodeId, x, y);
    }

    private static void relaxSignaturePlacements(Map<String, NodePlacement> placements) {
        Map<String, MutablePlacement> working = new LinkedHashMap<>();
        for (Map.Entry<String, NodePlacement> entry : placements.entrySet()) {
            NodePlacement point = entry.getValue();
            working.put(entry.getKey(), new MutablePlacement(point.x(), point.y(), point.x(), point.y()));
        }

        List<String> ids = new ArrayList<>(working.keySet());
        for (int iteration = 0; iteration < RELAX_ITERATIONS; iteration++) {
            Map<String, double[]> deltas = new LinkedHashMap<>();
            for (String id : ids) {
                deltas.put(id, new double[2]);
            }

            for (int i = 0; i < ids.size(); i++) {
                String aId = ids.get(i);
                MutablePlacement a = working.get(aId);
                boolean aMovable = isRelaxMovable(aId);
                for (int j = i + 1; j < ids.size(); j++) {
                    String bId = ids.get(j);
                    MutablePlacement b = working.get(bId);
                    boolean bMovable = isRelaxMovable(bId);
                    if (!aMovable && !bMovable) {
                        continue;
                    }

                    double dx = b.x - a.x;
                    double dy = b.y - a.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance >= MIN_NODE_SPACING) {
                        continue;
                    }

                    double nx;
                    double ny;
                    if (distance < 0.001) {
                        double angle = (Math.abs(aId.hashCode() ^ bId.hashCode()) % 360) * Math.PI / 180.0;
                        nx = Math.cos(angle);
                        ny = Math.sin(angle);
                        distance = 1.0;
                    } else {
                        nx = dx / distance;
                        ny = dy / distance;
                    }

                    double push = MIN_NODE_SPACING - distance;
                    double[] aDelta = deltas.get(aId);
                    double[] bDelta = deltas.get(bId);
                    if (aMovable && bMovable) {
                        aDelta[0] -= nx * push * 0.5;
                        aDelta[1] -= ny * push * 0.5;
                        bDelta[0] += nx * push * 0.5;
                        bDelta[1] += ny * push * 0.5;
                    } else if (aMovable) {
                        aDelta[0] -= nx * push;
                        aDelta[1] -= ny * push;
                    } else {
                        bDelta[0] += nx * push;
                        bDelta[1] += ny * push;
                    }
                }
            }

            for (String id : ids) {
                MutablePlacement point = working.get(id);
                if (!isRelaxMovable(id)) {
                    continue;
                }
                double[] delta = deltas.get(id);
                delta[0] += (point.originX - point.x) * RELAX_SPRING;
                delta[1] += (point.originY - point.y) * RELAX_SPRING;
                double step = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]);
                if (step > RELAX_MAX_STEP) {
                    double scale = RELAX_MAX_STEP / step;
                    delta[0] *= scale;
                    delta[1] *= scale;
                }
                point.x += delta[0];
                point.y += delta[1];
            }
        }

        for (Map.Entry<String, MutablePlacement> entry : working.entrySet()) {
            MutablePlacement point = entry.getValue();
            placements.put(entry.getKey(), new NodePlacement(
                    (int) Math.round(point.x),
                    (int) Math.round(point.y)));
        }
    }

    private static boolean isRelaxMovable(String nodeId) {
        MagicNode node = MagicTreeCatalog.byId(nodeId);
        return node != null && node.kind() == MagicNodeKind.SIGNATURE_SPELL;
    }

    private static void enforceRootHalo(Map<String, NodePlacement> placements, String rootId, int haloRadius) {
        NodePlacement root = placements.get(rootId);
        if (root == null) {
            return;
        }

        for (Map.Entry<String, NodePlacement> entry : placements.entrySet()) {
            if (rootId.equals(entry.getKey())) {
                continue;
            }

            NodePlacement placement = entry.getValue();
            int dx = placement.x() - root.x();
            int dy = placement.y() - root.y();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance >= haloRadius || distance < 0.001D) {
                continue;
            }

            double scale = haloRadius / distance;
            int nextX = root.x() + (int) Math.round(dx * scale);
            int nextY = root.y() + (int) Math.round(dy * scale);
            entry.setValue(new NodePlacement(nextX, nextY));
        }
    }

    private static void recenterOnVisualCenter(Map<String, NodePlacement> placements, NodePlacement targetCenter) {
        Bounds bounds = boundsOf(placements);
        int currentCenterX = (bounds.minX() + bounds.maxX()) / 2;
        int currentCenterY = (bounds.minY() + bounds.maxY()) / 2;
        int shiftX = targetCenter.x() - currentCenterX;
        int shiftY = targetCenter.y() - currentCenterY;

        if (bounds.minX() + shiftX < VIEWPORT_MIN_MARGIN_X) {
            shiftX += VIEWPORT_MIN_MARGIN_X - (bounds.minX() + shiftX);
        }
        if (bounds.minY() + shiftY < VIEWPORT_MIN_MARGIN_Y) {
            shiftY += VIEWPORT_MIN_MARGIN_Y - (bounds.minY() + shiftY);
        }

        if (shiftX == 0 && shiftY == 0) {
            return;
        }

        for (Map.Entry<String, NodePlacement> entry : placements.entrySet()) {
            NodePlacement placement = entry.getValue();
            entry.setValue(new NodePlacement(placement.x() + shiftX, placement.y() + shiftY));
        }
    }

    private static boolean isRoot(MagicNode node) {
        return "common/foundation/arcane_focus".equals(node.id());
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
        String base = baseDescriptionFor(node);
        String requirements = requirementsLineFor(node);
        return requirements.isEmpty() ? base : base + "\n\n" + requirements;
    }

    private static String baseDescriptionFor(MagicNode node) {
        return switch (node.id()) {
            case "common/foundation/arcane_focus" -> "Anchor your first arcane discipline and awaken the tree.";
            case "common/foundation/mana_well" -> "Deepen your reserves before branching into schools.";
            case "common/foundation/cast_discipline" -> "Refine control and unlock stronger magical paths.";
            case "common/foundation/multi_school_gate" -> "Bridge multiple schools and prepare for advanced branches.";
            default -> switch (node.kind()) {
                case BRANCH_OPENER -> "Open the " + titleCase(node.branch().id) + " school.";
                case BRANCH_TIER -> "Advance through tier " + tierNumber(node.tier()) + " of the "
                        + titleCase(node.branch().id) + " path.";
                case SIGNATURE_SPELL -> {
                    String spellDesc = null;
                    for (String spellId : node.learnedSpells()) {
                        spellDesc = SpellDescriptionProvider.get(spellId);
                        if (spellDesc != null) break;
                    }
                    yield spellDesc != null ? spellDesc : "Learn " + titleFor(node) + ".";
                }
                case TRUNK_FOUNDATION, LATEGAME_GATE -> "Progress your unified magic tree.";
            };
        };
    }

    /**
     * Construit le bloc de requirements lisible pour le tooltip Puffish — extrait les
     * descriptions du {@link Condition} tree du node. Pour le vrai check runtime avec
     * ajustements race, voir {@link tong.statmod.magic.MagicEligibilityResolver}.
     */
    private static String requirementsLineFor(MagicNode node) {
        tong.statmod.magic.Condition cond = node.condition();
        if (cond == null) return "";
        StringBuilder sb = new StringBuilder("Requirements:");
        describeCondition(sb, cond, 0);
        return sb.toString();
    }

    private static void describeCondition(StringBuilder sb, tong.statmod.magic.Condition cond, int depth) {
        switch (cond) {
            case tong.statmod.magic.Condition.And and -> {
                for (tong.statmod.magic.Condition c : and.children()) {
                    describeCondition(sb, c, depth);
                }
            }
            case tong.statmod.magic.Condition.Or or -> {
                sb.append("\n").append("  ".repeat(depth)).append("• One of:");
                for (tong.statmod.magic.Condition c : or.children()) {
                    describeCondition(sb, c, depth + 1);
                }
            }
            case tong.statmod.magic.Condition.StatCondition sc ->
                sb.append("\n").append("  ".repeat(depth)).append("• ")
                  .append(sc.stat().displayName).append(" ≥ ").append(sc.minLevel());
            case tong.statmod.magic.Condition.RaceCondition rc ->
                sb.append("\n").append("  ".repeat(depth)).append("• Race: ").append(rc.race().name());
            case tong.statmod.magic.Condition.HasSpellCondition hsc ->
                sb.append("\n").append("  ".repeat(depth)).append("• Spell: ").append(hsc.spellId());
            case tong.statmod.magic.Condition.HasNodeCondition hnc ->
                sb.append("\n").append("  ".repeat(depth)).append("• Node: ").append(hnc.nodeId());
            case tong.statmod.magic.Condition.BranchTierCondition btc ->
                sb.append("\n").append("  ".repeat(depth)).append("• ").append(btc.branch().id)
                  .append(" tier ≥ ").append(btc.minTier());
            case tong.statmod.magic.Condition.GlobalLevelCondition glc ->
                sb.append("\n").append("  ".repeat(depth)).append("• Global Level ≥ ").append(glc.minLevel());
        }
    }

    private static IconSpec iconFor(MagicNode node) {
        if (node == null) {
            return IconSpec.item("minecraft:enchanted_book");
        }
        return switch (node.id()) {
            case "common/foundation/arcane_focus" -> IconSpec.item("minecraft:ender_pearl");
            case "common/foundation/mana_well" -> IconSpec.item("minecraft:experience_bottle");
            case "common/foundation/cast_discipline" -> IconSpec.item("minecraft:book");
            case "common/foundation/multi_school_gate" -> IconSpec.item("minecraft:nether_star");
            default -> {
                String texture = textureIconFor(node);
                if (texture != null) {
                    yield IconSpec.texture(texture);
                }
                if (node.kind() == MagicNodeKind.BRANCH_OPENER) {
                    yield IconSpec.item(openerIcon(node.branch()));
                }
                if (node.kind() == MagicNodeKind.BRANCH_TIER) {
                    yield IconSpec.item(tierIcon(node.branch(), node.tier()));
                }
                yield IconSpec.item(signatureItemFallback(node));
            }
        };
    }

    private static String textureIconFor(MagicNode node) {
        if (node == null) {
            return null;
        }
        if (node.kind() == MagicNodeKind.SIGNATURE_SPELL) {
            for (String spellId : sortedSpellIds(node.learnedSpells())) {
                String texture = DISCOVERED_SPELL_TEXTURES.get(spellId);
                if (texture != null) {
                    return texture;
                }
            }
        }
        String representativeSpellId = representativeSpellId(node);
        return representativeSpellId == null ? null : DISCOVERED_SPELL_TEXTURES.get(representativeSpellId);
    }

    private static List<String> sortedSpellIds(Set<String> spellIds) {
        List<String> sorted = new ArrayList<>(spellIds);
        sorted.sort(Comparator
                .comparingInt(PuffishMagicTreeBuilder::spellNamespacePriority)
                .thenComparing(String::toString));
        return sorted;
    }

    private static int spellNamespacePriority(String spellId) {
        if (spellId == null) {
            return Integer.MAX_VALUE;
        }
        String namespace = spellId.contains(":") ? spellId.substring(0, spellId.indexOf(':')) : "";
        return switch (namespace) {
            case "tensura" -> 0;
            case "darkdoppelganger" -> 1;
            case "legendarymage" -> 2;
            case "gametechbcs_spellbooks" -> 3;
            case "wind_spellbooks" -> 4;
            case "spells_gone_wrong" -> 5;
            case "irons_spellbooks" -> 6;
            default -> 7;
        };
    }

    private static String representativeSpellId(MagicNode node) {
        if (node == null) {
            return null;
        }
        return switch (node.branch()) {
            case FIRE -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:firebolt";
                case T2 -> "irons_spellbooks:fireball";
                case T3, T4 -> "gametechbcs_spellbooks:meteor_storm";
            };
            case WATER -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:snowball";
                case T2 -> "irons_spellbooks:ray_of_frost";
                case T3, T4 -> "irons_spellbooks:blizzard";
            };
            case AIR -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:charge";
                case T2 -> "irons_spellbooks:chain_lightning";
                case T3, T4 -> "irons_spellbooks:thunderstorm";
            };
            case EARTH -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:poison_arrow";
                case T2 -> "irons_spellbooks:acid_orb";
                case T3, T4 -> "irons_spellbooks:earthquake";
            };
            case HOLY -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:heal";
                case T2 -> "irons_spellbooks:healing_circle";
                case T3, T4 -> "irons_spellbooks:sunbeam";
            };
            case BLOOD -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:acupuncture";
                case T2 -> "irons_spellbooks:devour";
                case T3, T4 -> "legendarymage:resurrection_rune";
            };
            case ENDER -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:magic_arrow";
                case T2 -> "irons_spellbooks:portal";
                case T3, T4 -> "irons_spellbooks:black_hole";
            };
            case EVOCATION -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:gust";
                case T2 -> "irons_spellbooks:invisibility";
                case T3, T4 -> "irons_spellbooks:chain_creeper";
            };
            case ELDRITCH -> switch (node.tier()) {
                case T1 -> "irons_spellbooks:eldritch_blast";
                case T2 -> "irons_spellbooks:telekinesis";
                case T3, T4 -> "irons_spellbooks:abyssal_shroud";
            };
            case COMMON -> null;
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

    private static String signatureItemFallback(MagicNode node) {
        String slug = node.id().substring(node.id().lastIndexOf('/') + 1);
        if (containsAllTokens(slug, "fireball")) return "minecraft:fire_charge";
        if (containsAllTokens(slug, "fire", "breath")) return "minecraft:dragon_breath";
        if (containsAllTokens(slug, "hellfire")) return "minecraft:lava_bucket";
        if (containsAllTokens(slug, "burning", "dash")) return "minecraft:magma_cream";
        if (containsAnyToken(slug, "snow", "frost", "ice", "blizzard")) return "minecraft:snowball";
        if (containsAnyToken(slug, "lightning", "thunder", "shock", "volt")) return "minecraft:lightning_rod";
        if (containsAnyToken(slug, "poison", "acid", "root", "oakskin")) return "minecraft:spider_eye";
        if (containsAnyToken(slug, "heal", "healing", "regeneration", "blessing", "sunbeam")) return "minecraft:golden_apple";
        if (containsAnyToken(slug, "blood", "siphon", "siphoning", "sacrifice", "wither")) return "minecraft:redstone";
        if (containsAnyToken(slug, "portal", "teleport", "ender", "black", "hole")) return "minecraft:ender_pearl";
        if (containsAnyToken(slug, "vex", "fang", "wololo", "creeper")) return "minecraft:totem_of_undying";
        if (containsAnyToken(slug, "eldritch", "sculk", "abyss", "abyssal", "dimension")) return "minecraft:echo_shard";
        if (containsAnyToken(slug, "tensura")) return branchSignatureFallback(node.branch());
        return branchSignatureFallback(node.branch());
    }

    private static boolean containsAnyToken(String slug, String... tokens) {
        for (String token : tokens) {
            if (containsAllTokens(slug, token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAllTokens(String slug, String... tokens) {
        String[] words = slug.split("_");
        for (String token : tokens) {
            boolean found = false;
            for (String word : words) {
                if (word.equals(token)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
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

    private static String visualParentId(MagicNode node) {
        if (node == null || isRoot(node)) {
            return null;
        }
        return switch (node.id()) {
            default -> {
                if (node.kind() == MagicNodeKind.BRANCH_OPENER) {
                    yield "common/foundation/arcane_focus";
                }
                for (String prerequisite : node.prerequisites()) {
                    if (MagicTreeCatalog.LOCKED_SENTINEL.equals(prerequisite)) {
                        continue;
                    }
                    if (prerequisite.startsWith(node.branch().id + "/")) {
                        yield prerequisite;
                    }
                }
                yield node.prerequisites().isEmpty() ? null : node.prerequisites().get(0);
            }
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

    private static int radialDistance(int x, int y) {
        int localX = x - CANVAS_OFFSET_X;
        int localY = y - CANVAS_OFFSET_Y;
        int dx = localX - CONSTELLATION_CENTER_X;
        int dy = localY - CONSTELLATION_CENTER_Y;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy));
    }

    private static float sizeFor(MagicNode node) {
        if (node == null) {
            return 1.0f;
        }
        return switch (node.id()) {
            case "common/foundation/arcane_focus" -> 2.8f;
            case "common/foundation/mana_well",
                    "common/foundation/cast_discipline" -> 1.95f;
            case "common/foundation/multi_school_gate" -> 2.1f;
            default -> switch (node.kind()) {
                case BRANCH_OPENER -> 1.55f;
                case BRANCH_TIER -> 1.2f;
                case SIGNATURE_SPELL, TRUNK_FOUNDATION, LATEGAME_GATE -> 0.96f;
            };
        };
    }

    private static String formatSize(float size) {
        String raw = String.format(Locale.ROOT, "%.2f", size);
        while (raw.contains(".") && (raw.endsWith("0") || raw.endsWith("."))) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    private static String escape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static Map<MagicBranch, BranchLayout> buildBranchLayouts() {
        Map<MagicBranch, BranchLayout> layouts = new EnumMap<>(MagicBranch.class);
        layouts.put(MagicBranch.AIR,   new BranchLayout(305.0, 300, 145,
                72.0, 18.0, 145.0, 90.0, -1, 20.0, 70.0));
        layouts.put(MagicBranch.FIRE,  new BranchLayout(235.0, 310, 150,
                64.0, 10.0, 150.0, 86.0, -1, 18.0, 64.0));
        layouts.put(MagicBranch.WATER, new BranchLayout(125.0, 300, 145,
                68.0, 18.0, 125.0, 84.0, 1, 18.0, 64.0));
        layouts.put(MagicBranch.EARTH, new BranchLayout(55.0, 285, 135,
                52.0, 8.0, 115.0, 72.0, 1, 14.0, 42.0));
        layouts.put(MagicBranch.HOLY,      new BranchLayout(330.0, 330, 155,
                62.0, 24.0, 140.0, 86.0, -1, 18.0, 60.0));
        layouts.put(MagicBranch.BLOOD,     new BranchLayout(18.0, 320, 150,
                70.0, 28.0, 140.0, 90.0, 1, 20.0, 66.0));
        layouts.put(MagicBranch.ENDER,     new BranchLayout(90.0, 320, 150,
                66.0, 22.0, 110.0, 80.0, 1, 20.0, 58.0));
        layouts.put(MagicBranch.EVOCATION, new BranchLayout(162.0, 320, 150,
                72.0, 24.0, -60.0, 92.0, -1, 20.0, 74.0));
        layouts.put(MagicBranch.ELDRITCH,  new BranchLayout(210.0, 330, 155,
                60.0, 24.0, 142.0, 90.0, -1, 18.0, 62.0));
        return layouts;
    }

    private static Map<String, String> discoverSpellTextures() {
        Map<String, String> textures = new LinkedHashMap<>();
        scanSpellTextures(LIBS_DIR, textures);
        scanSpellTextures(RUN_MODS_DIR, textures);
        return textures;
    }

    private static void scanSpellTextures(Path directory, Map<String, String> textures) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try {
            try (var jars = Files.list(directory)) {
                jars.filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .forEach(path -> scanSpellTexturesInJar(path, textures));
            }
        } catch (IOException ignored) {
        }
    }

    private static void scanSpellTexturesInJar(Path jarPath, Map<String, String> textures) {
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            zip.stream()
                    .map(entry -> entry.getName())
                    .filter(PuffishMagicTreeBuilder::isSpellTextureEntry)
                    .forEach(entry -> registerTextureEntry(entry, textures));
        } catch (IOException ignored) {
        }
    }

    private static boolean isSpellTextureEntry(String entry) {
        if (entry == null || !entry.startsWith("assets/") || !entry.endsWith(".png")) {
            return false;
        }
        if (entry.contains("/textures/gui/spell_icons/")) {
            return true;
        }
        return entry.startsWith("assets/tensura/textures/magic/")
                || entry.startsWith("assets/tensura/textures/battlewill/");
    }

    private static void registerTextureEntry(String entry, Map<String, String> textures) {
        String[] parts = entry.split("/");
        if (parts.length < 4) {
            return;
        }
        String namespace = parts[1];
        String relativePath = entry.substring(("assets/" + namespace + "/").length());
        String fileName = parts[parts.length - 1];
        if (!fileName.endsWith(".png")) {
            return;
        }
        String stem = fileName.substring(0, fileName.length() - 4);
        textures.putIfAbsent(namespace + ":" + stem, namespace + ":" + relativePath);
    }

    record LayoutSnapshot(
            Map<String, NodePlacement> placements,
            Bounds bounds,
            NodePlacement root,
            NodePlacement boundsCenter
    ) {}

    record Bounds(int minX, int maxX, int minY, int maxY) {}

    private record BranchLayout(
            double angleDegrees,
            int openerRadius,
            int tierRadiusStep,
            double signatureLaneStep,
            double signatureSideOffset,
            double signatureForwardOffset,
            double signatureRowDepth,
            int signatureSideBias,
            double clusterFanDegrees,
            double clusterTangentSeparation
    ) {}

    record NodePlacement(int x, int y) {}

    private static final class MutablePlacement {
        private double x;
        private double y;
        private final double originX;
        private final double originY;

        private MutablePlacement(double x, double y, double originX, double originY) {
            this.x = x;
            this.y = y;
            this.originX = originX;
            this.originY = originY;
        }
    }

    private record IconSpec(String type, String key, String value) {
        static IconSpec item(String itemId) {
            return new IconSpec("item", "item", itemId);
        }

        static IconSpec texture(String textureId) {
            return new IconSpec("texture", "texture", textureId);
        }

        String jsonBlock() {
            return "        \"icon\": {\n" +
                    "            \"type\": \"" + type + "\",\n" +
                    "            \"data\": {\n" +
                    "                \"" + key + "\": \"" + value + "\"\n" +
                    "            }\n" +
                    "        }";
        }
    }
}
