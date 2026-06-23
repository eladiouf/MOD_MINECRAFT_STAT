package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishMagicResourceConsistencyTest {
    @Test
    void unified_magic_category_is_declared_and_backed_by_resources() throws Exception {
        ClassLoader loader = PuffishMagicResourceConsistencyTest.class.getClassLoader();

        String configJson;
        try (InputStream stream = loader.getResourceAsStream("data/statmod/puffish_skills/config.json")) {
            assertNotNull(stream, "missing puffish config");
            configJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        for (MagicNode node : MagicTreeCatalog.all()) {
            String categoryId = PuffishMagicCategoryIds.categoryFor(node.id());
            assertNotNull(categoryId, "node must map to a Puffish category: " + node.id());
            assertTrue("statmod:statmod_magic".equals(categoryId),
                    "node must resolve to the unified magic category: " + node.id());
        }

        String category = "statmod_magic";
        assertTrue(configJson.contains('"' + category + '"'),
                "config must declare category " + category);
        assertHasResource(loader, category, "category.json");
        assertHasResource(loader, category, "definitions.json");
        assertHasResource(loader, category, "skills.json");
        assertHasResource(loader, category, "connections.json");

        try (InputStream stream = loader.getResourceAsStream(
                "data/statmod/puffish_skills/categories/statmod_magic/definitions.json")) {
            assertNotNull(stream, "missing unified magic definitions");
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (MagicNode node : MagicTreeCatalog.all()) {
                String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
                assertTrue(json.contains("\"" + skillId + "\""),
                        "missing magic definition for node " + node.id());
            }
            assertTrue(json.contains("\"common.foundation.arcane_focus\""));
            assertTrue(json.contains("\"fire.signature.fireball\""));
            assertTrue(json.contains("\"water.signature.blizzard\""));
            assertTrue(json.contains("\"blood.signature.sacrifice\""));
            assertTrue(json.contains("\"eldritch.signature.pocket_dimension\""));
            assertTrue(json.contains("\"holy.signature.tensura_magic_barrier\""));
            assertTrue(json.contains("\"ender.signature.tensura_teleport\""));
            assertTrue(json.contains("\"evocation.signature.tensura_analyze\""));
            assertTrue(json.contains("\"eldritch.signature.tensura_true_darkness\""));
            assertDefinitionUsesTexture(json, "blood.signature.sacrifice",
                    "irons_spellbooks:textures/gui/spell_icons/sacrifice.png");
        }

        try (InputStream stream = loader.getResourceAsStream(
                "data/statmod/puffish_skills/categories/statmod_magic/skills.json")) {
            assertNotNull(stream, "missing unified magic skills");
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (MagicNode node : MagicTreeCatalog.all()) {
                String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
                assertTrue(json.contains("\"" + skillId + "\": {"),
                        "missing generated magic skill placement for node " + node.id());
            }
            assertTrue(json.contains("\"common.foundation.arcane_focus\": {"),
                    "the common arcane focus should remain present in the unified tree");
            assertEquals(1, countOccurrences(json, "\"root\": true"),
                    "the unified magic tab should expose a single shared root");
            assertTrue(minCoordinate(json, "\"x\": (-?\\d+)") >= 0,
                    "all generated magic nodes should stay inside the positive canvas on X");
            assertTrue(minCoordinate(json, "\"y\": (-?\\d+)") >= 0,
                    "all generated magic nodes should stay inside the positive canvas on Y");
        }

        try (InputStream stream = loader.getResourceAsStream(
                "data/statmod/puffish_skills/categories/statmod_magic_common/category.json")) {
            assertTrue(stream == null, "legacy split magic categories should be removed");
        }
    }

    @Test
    void generated_unified_magic_tree_stays_within_compact_canvas() {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        String json = files.skillsJson();
        assertTrue(maxCoordinate(json, "\"x\": (-?\\d+)") <= 2200,
                "the unified magic tree should stay horizontally compact enough for one readable tab");
        assertTrue(maxCoordinate(json, "\"y\": (-?\\d+)") <= 2200,
                "the unified magic tree should stay vertically compact enough for one readable tab");
    }

    @Test
    void generated_unified_magic_tree_starts_close_enough_to_left_edge_for_default_viewport() {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        String json = files.skillsJson();
        assertTrue(minCoordinate(json, "\"x\": (-?\\d+)") <= 80,
                "the unified magic tree should not look shifted to the right in Puffish's default viewport");
    }

    @Test
    void generated_unified_magic_tree_keeps_opposed_school_openers_in_mirror() {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        String json = files.skillsJson();
        int centerX = coordinateFor(json, "common.foundation.arcane_focus", "x");
        assertMirrored(json, centerX, "fire.opener.ignition", "water.opener.ice_awakening");
        assertMirrored(json, centerX, "air.opener.spark_awakening", "earth.opener.nature_awakening");
        assertMirrored(json, centerX, "holy.opener.light_awakening", "eldritch.opener.dark_awakening");
        assertMirrored(json, centerX, "blood.opener.sanguine_awakening", "evocation.opener.trick_awakening");
    }

    @Test
    void generated_unified_magic_tree_keeps_each_school_in_a_local_cluster() {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        String json = files.skillsJson();
        for (MagicBranch branch : MagicBranch.values()) {
            if (branch == MagicBranch.COMMON) {
                continue;
            }
            Bounds bounds = boundsForBranch(json, branch.id);
            assertTrue(bounds.width() <= 620, "branch " + branch + " should stay horizontally localized");
            assertTrue(bounds.height() <= 760, "branch " + branch + " should stay vertically localized");
        }
    }

    @Test
    void generated_unified_magic_tree_uses_one_visual_parent_per_non_root_node() {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        String json = files.connectionsJson();
        assertEquals(MagicTreeCatalog.all().size() - 1, countConnectionPairs(json),
                "the Puffish mirror should show one clean visual parent per non-root node");
    }

    @Test
    void generated_unified_magic_tree_keeps_nodes_visually_separated() {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        String json = files.skillsJson();
        ClosestPair closestPair = closestPair(json);
        double minDistance = closestPair.distance();
        assertTrue(minDistance >= 42.0,
                "the unified magic tree should keep enough space between nodes to avoid visual overlap, actual min="
                        + minDistance + " between " + closestPair.a().id() + " and " + closestPair.b().id());
    }

    @Test
    void generated_unified_magic_definitions_emphasize_the_core_and_use_real_spell_textures() {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        String json = files.definitionsJson();

        assertTrue(sizeForDefinition(json, "common.foundation.arcane_focus") > 2.0f,
                "the central root should render larger than standard nodes");
        assertTrue(sizeForDefinition(json, "common.foundation.multi_school_gate") > 1.7f,
                "the late common gate should remain visually prominent");

        assertDefinitionUsesTexture(json, "fire.signature.fireball",
                "irons_spellbooks:textures/gui/spell_icons/fireball.png");
        assertDefinitionUsesTexture(json, "earth.signature.acid_rain",
                "gametechbcs_spellbooks:textures/gui/spell_icons/acid_rain.png");
        assertDefinitionUsesTexture(json, "air.signature.wind_blade",
                "wind_spellbooks:textures/gui/spell_icons/wind_blade.png");
        assertDefinitionUsesTexture(json, "ender.signature.doppel_portal",
                "darkdoppelganger:textures/gui/spell_icons/doppel_portal.png");
        assertDefinitionUsesTexture(json, "evocation.signature.nucreeper_strike",
                "spells_gone_wrong:textures/gui/spell_icons/nucreeper_strike.png");
        assertDefinitionUsesTexture(json, "water.signature.legendary_blizzard",
                "legendarymage:textures/gui/spell_icons/blizzard.png");
        assertDefinitionUsesTexture(json, "fire.signature.tensura_fire_ball",
                "tensura:textures/magic/aspectual/fire_ball.png");
    }

    private static void assertHasResource(ClassLoader loader, String category, String fileName) {
        String path = "data/statmod/puffish_skills/categories/" + category + "/" + fileName;
        try (InputStream stream = loader.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
        } catch (Exception e) {
            throw new AssertionError("failed to read resource " + path, e);
        }
    }

    private static int minCoordinate(String json, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        int min = Integer.MAX_VALUE;
        while (matcher.find()) {
            min = Math.min(min, Integer.parseInt(matcher.group(1)));
        }
        return min;
    }

    private static int maxCoordinate(String json, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        int max = Integer.MIN_VALUE;
        while (matcher.find()) {
            max = Math.max(max, Integer.parseInt(matcher.group(1)));
        }
        return max;
    }

    private static int countOccurrences(String json, String needle) {
        int count = 0;
        int index = 0;
        while ((index = json.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static int countConnectionPairs(String json) {
        Matcher matcher = Pattern.compile("\\[\\s+\"[^\"]+\",\\s+\"[^\"]+\"\\s+\\]").matcher(json);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static void assertMirrored(String json, int centerX, String leftNode, String rightNode) {
        int leftX = coordinateFor(json, leftNode, "x");
        int rightX = coordinateFor(json, rightNode, "x");
        int leftY = coordinateFor(json, leftNode, "y");
        int rightY = coordinateFor(json, rightNode, "y");
        assertEquals(leftY, rightY, "opposed school openers should stay on the same row");
        assertEquals(centerX * 2, leftX + rightX,
                "opposed school openers should stay mirrored around the common trunk");
    }

    private static int coordinateFor(String json, String skillId, String axis) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(skillId) + "\": \\{.*?\"" + axis + "\": (-?\\d+)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        assertTrue(matcher.find(), "missing coordinate for " + skillId + " / " + axis);
        return Integer.parseInt(matcher.group(1));
    }

    private static Bounds boundsForBranch(String json, String branchId) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(branchId) + "\\.[^\"]+\": \\{\\s+\"x\": (-?\\d+),\\s+\"y\": (-?\\d+)",
                Pattern.MULTILINE).matcher(json);
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        while (matcher.find()) {
            xs.add(Integer.parseInt(matcher.group(1)));
            ys.add(Integer.parseInt(matcher.group(2)));
        }
        assertFalse(xs.isEmpty(), "missing nodes for branch " + branchId);
        int minX = xs.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int maxX = xs.stream().mapToInt(Integer::intValue).max().orElseThrow();
        int minY = ys.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int maxY = ys.stream().mapToInt(Integer::intValue).max().orElseThrow();
        return new Bounds(minX, maxX, minY, maxY);
    }

    private static double minPairwiseDistance(String json) {
        return closestPair(json).distance();
    }

    private static ClosestPair closestPair(String json) {
        List<Point> points = parsePoints(json);
        double min = Double.MAX_VALUE;
        Point closestA = null;
        Point closestB = null;
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                Point a = points.get(i);
                Point b = points.get(j);
                int dx = a.x() - b.x();
                int dy = a.y() - b.y();
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < min) {
                    min = distance;
                    closestA = a;
                    closestB = b;
                }
            }
        }
        return new ClosestPair(closestA, closestB, min);
    }

    private static List<Point> parsePoints(String json) {
        Matcher matcher = Pattern.compile("\"([^\"]+)\": \\{\\s+\"x\": (-?\\d+),\\s+\"y\": (-?\\d+)",
                Pattern.MULTILINE).matcher(json);
        List<Point> points = new ArrayList<>();
        while (matcher.find()) {
            points.add(new Point(matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))));
        }
        return points;
    }

    private static float sizeForDefinition(String json, String skillId) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(skillId) + "\": \\{.*?\"size\": ([0-9]+(?:\\.[0-9]+)?)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        assertTrue(matcher.find(), "missing size for " + skillId);
        return Float.parseFloat(matcher.group(1));
    }

    private static void assertDefinitionUsesTexture(String json, String skillId, String texture) {
        String normalized = json.toLowerCase(Locale.ROOT);
        String block = "\"" + skillId + "\": {";
        int start = normalized.indexOf(block);
        assertTrue(start >= 0, "missing definition for " + skillId);
        int end = normalized.indexOf("\n    }", start);
        assertTrue(end > start, "unable to isolate definition for " + skillId);
        String definition = normalized.substring(start, end);
        assertTrue(definition.contains("\"type\": \"texture\""),
                skillId + " should use a texture icon");
        assertTrue(definition.contains("\"texture\": \"" + texture.toLowerCase(Locale.ROOT) + "\""),
                skillId + " should use texture " + texture);
    }

    private record Bounds(int minX, int maxX, int minY, int maxY) {
        int width() {
            return maxX - minX;
        }

        int height() {
            return maxY - minY;
        }
    }

    private record Point(String id, int x, int y) {}

    private record ClosestPair(Point a, Point b, double distance) {}
}
