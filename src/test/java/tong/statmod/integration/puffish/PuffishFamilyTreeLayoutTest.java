package tong.statmod.integration.puffish;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishFamilyTreeLayoutTest {
    @Test
    void generated_unified_perk_tree_uses_positive_canvas_coordinates() {
        List<SkillNode> nodes = parseSkills(PuffishFamilyTreeBuilder.unifiedCategoryFiles().skillsJson());
        int minX = nodes.stream().mapToInt(SkillNode::x).min().orElseThrow();
        int minY = nodes.stream().mapToInt(SkillNode::y).min().orElseThrow();
        assertTrue(minX >= 40, "perk tree should keep a safe left margin, actual minX=" + minX);
        assertTrue(minY >= 40, "perk tree should keep a safe top margin, actual minY=" + minY);
    }

    @Test
    void generated_unified_perk_tree_keeps_core_nodes_in_a_readable_central_halo() {
        List<SkillNode> nodes = parseSkills(PuffishFamilyTreeBuilder.unifiedCategoryFiles().skillsJson());
        List<SkillNode> cores = nodes.stream().filter(SkillNode::root).toList();
        ClosestPair closestPair = closestPair(cores);
        assertTrue(closestPair.distance() >= 72.0,
                "core nodes should not collapse near the center, actual min="
                        + closestPair.distance() + " between " + closestPair.a().id() + " and " + closestPair.b().id());
    }

    @Test
    void generated_unified_perk_tree_keeps_all_nodes_visually_separated() {
        List<SkillNode> nodes = parseSkills(PuffishFamilyTreeBuilder.unifiedCategoryFiles().skillsJson());
        ClosestPair closestPair = closestPair(nodes);
        assertTrue(closestPair.distance() >= 56.0,
                "perk nodes should keep enough spacing to avoid overlaps, actual min="
                        + closestPair.distance() + " between " + closestPair.a().id() + " and " + closestPair.b().id());
    }

    private static List<SkillNode> parseSkills(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<SkillNode> nodes = new ArrayList<>();
        for (var entry : root.entrySet()) {
            JsonObject node = entry.getValue().getAsJsonObject();
            nodes.add(new SkillNode(
                    entry.getKey(),
                    node.get("x").getAsInt(),
                    node.get("y").getAsInt(),
                    node.has("root") && node.get("root").getAsBoolean()
            ));
        }
        nodes.sort(Comparator.comparing(SkillNode::id));
        return nodes;
    }

    private static ClosestPair closestPair(List<SkillNode> nodes) {
        double minDistance = Double.POSITIVE_INFINITY;
        SkillNode bestA = null;
        SkillNode bestB = null;
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                SkillNode a = nodes.get(i);
                SkillNode b = nodes.get(j);
                double distance = Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2));
                if (distance < minDistance) {
                    minDistance = distance;
                    bestA = a;
                    bestB = b;
                }
            }
        }
        return new ClosestPair(bestA, bestB, minDistance);
    }

    private record SkillNode(String id, int x, int y, boolean root) {}

    private record ClosestPair(SkillNode a, SkillNode b, double distance) {}
}
