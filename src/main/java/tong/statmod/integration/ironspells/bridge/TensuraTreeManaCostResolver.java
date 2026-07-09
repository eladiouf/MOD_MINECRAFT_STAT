package tong.statmod.integration.ironspells.bridge;

import tong.statmod.integration.tensura.TensuraSkillIds;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTier;
import tong.statmod.magic.MagicTreeCatalog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class TensuraTreeManaCostResolver {
    private static final Map<String, CostBand> TREE_COSTS = buildTreeCosts();

    private TensuraTreeManaCostResolver() {}

    static Integer resolve(String skillId, String discipline) {
        if (skillId == null || skillId.isBlank()) {
            return null;
        }

        String canonicalSkillId = TensuraSkillIds.canonicalize(skillId);
        if (canonicalSkillId == null || !canonicalSkillId.startsWith("tensura:")) {
            return null;
        }

        CostBand band = TREE_COSTS.get(canonicalSkillId);
        if (band == null) {
            return null;
        }

        int tierBase = switch (band.tier()) {
            case T1 -> 18;
            case T2 -> 28;
            case T3 -> 42;
            case T4 -> 56;
        };
        int nodeCostStep = Math.max(0, band.nodeCost() - 1) * 8;
        int disciplineOffset = switch (discipline == null ? "" : discipline) {
            case "utility" -> -4;
            case "mobility" -> -3;
            case "support" -> -2;
            case "defense" -> 0;
            case "empowerment" -> 3;
            case "control" -> 4;
            case "pressure" -> 6;
            case "offense" -> 8;
            default -> 0;
        };
        return Math.max(10, tierBase + nodeCostStep + disciplineOffset);
    }

    private static Map<String, CostBand> buildTreeCosts() {
        Map<String, CostBand> costs = new ConcurrentHashMap<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            for (String spellId : node.learnedSpells()) {
                if (spellId == null || spellId.isBlank()) {
                    continue;
                }
                String canonicalSkillId = TensuraSkillIds.canonicalize(spellId);
                if (canonicalSkillId == null || !canonicalSkillId.startsWith("tensura:")) {
                    continue;
                }
                costs.putIfAbsent(canonicalSkillId, new CostBand(node.tier(), node.cost()));
            }
        }
        return Map.copyOf(costs);
    }

    private record CostBand(MagicTier tier, int nodeCost) {}
}
