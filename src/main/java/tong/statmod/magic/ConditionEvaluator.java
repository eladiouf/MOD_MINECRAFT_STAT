package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

public final class ConditionEvaluator {

    public static boolean evaluate(MagicNode node, PlayerStatData data) {
        if (node.condition() == null) return true;
        return node.condition().evaluate(data, ConditionContext.defaultContext());
    }

    public static List<String> describeMissing(MagicNode node, PlayerStatData data) {
        if (node.condition() == null) return List.of();
        return collectMissing(node.condition(), data, ConditionContext.defaultContext());
    }

    private static List<String> collectMissing(Condition cond, PlayerStatData data, ConditionContext ctx) {
        return switch (cond) {
            case Condition.And and -> {
                List<String> result = new ArrayList<>();
                for (Condition c : and.children()) {
                    result.addAll(collectMissing(c, data, ctx));
                }
                yield result;
            }
            case Condition.Or or -> {
                for (Condition c : or.children()) {
                    if (c.evaluate(data, ctx)) yield List.of();
                }
                List<String> result = new ArrayList<>();
                for (Condition c : or.children()) {
                    result.addAll(collectMissing(c, data, ctx));
                }
                yield result;
            }
            case Condition.StatCondition sc -> {
                if (sc.evaluate(data, ctx)) yield List.of();
                yield List.of(sc.stat().displayName + " \u2265 " + sc.minLevel());
            }
            case Condition.RaceCondition rc -> {
                if (rc.evaluate(data, ctx)) yield List.of();
                yield List.of("Race: " + rc.race().name());
            }
            case Condition.HasSpellCondition hsc -> {
                if (hsc.evaluate(data, ctx)) yield List.of();
                yield List.of("Spell: " + hsc.spellId());
            }
            case Condition.HasNodeCondition hnc -> {
                if (hnc.evaluate(data, ctx)) yield List.of();
                yield List.of("Node: " + hnc.nodeId());
            }
            case Condition.BranchTierCondition btc -> {
                if (btc.evaluate(data, ctx)) yield List.of();
                yield List.of(btc.branch().id + " tier \u2265 " + btc.minTier());
            }
            case Condition.GlobalLevelCondition glc -> {
                if (glc.evaluate(data, ctx)) yield List.of();
                yield List.of("Global Level \u2265 " + glc.minLevel());
            }
        };
    }

    private ConditionEvaluator() {}
}
