package tong.statmod.magic;

import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.List;
import java.util.Objects;

public sealed interface Condition {

    boolean evaluate(PlayerStatData data, ConditionContext ctx);

    record And(List<Condition> children) implements Condition {
        public And {
            children = List.copyOf(children);
            if (children.isEmpty()) throw new IllegalArgumentException("And must have at least one child");
        }
        public static And of(Condition... cs) { return new And(List.of(cs)); }

        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            for (Condition c : children) {
                if (!c.evaluate(data, ctx)) return false;
            }
            return true;
        }
    }

    record Or(List<Condition> children) implements Condition {
        public Or {
            children = List.copyOf(children);
            if (children.isEmpty()) throw new IllegalArgumentException("Or must have at least one child");
        }
        public static Or of(Condition... cs) { return new Or(List.of(cs)); }

        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            for (Condition c : children) {
                if (c.evaluate(data, ctx)) return true;
            }
            return false;
        }
    }

    record StatCondition(StatType stat, int minLevel) implements Condition {
        public StatCondition {
            Objects.requireNonNull(stat);
            if (minLevel < 0) throw new IllegalArgumentException("minLevel must be >= 0");
        }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.getLevel(stat.index) >= minLevel;
        }
    }

    record RaceCondition(MagicRace race) implements Condition {
        public RaceCondition { Objects.requireNonNull(race); }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.getMagicRace() == race;
        }
    }

    record HasSpellCondition(String spellId) implements Condition {
        public HasSpellCondition { Objects.requireNonNull(spellId); }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.hasLearnedSpell(spellId);
        }
    }

    record HasNodeCondition(String nodeId) implements Condition {
        public HasNodeCondition { Objects.requireNonNull(nodeId); }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.hasMagicNode(nodeId);
        }
    }

    record BranchTierCondition(MagicBranch branch, MagicTier minTier) implements Condition {
        public BranchTierCondition {
            Objects.requireNonNull(branch);
            Objects.requireNonNull(minTier);
        }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return ctx.highestTierInBranch(data, branch) != null
                    && ctx.highestTierInBranch(data, branch).compareTo(minTier) >= 0;
        }
    }

    record GlobalLevelCondition(int minLevel) implements Condition {
        public GlobalLevelCondition {
            if (minLevel < 0) throw new IllegalArgumentException("minLevel must be >= 0");
        }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.getGlobalLevel() >= minLevel;
        }
    }
}
