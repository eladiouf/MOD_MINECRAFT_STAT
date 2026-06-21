package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

public final class MagicEligibilityResolver {
    public enum Failure { NONE, LOCKED, MISSING_PREREQ, NOT_ENOUGH_POINTS, ALREADY_UNLOCKED, NO_RACE }

    public record Result(Failure failure, int adjustedCost) {}

    private MagicEligibilityResolver() {}

    public static Result evaluate(PlayerStatData data, MagicNode node) {
        if (data == null || node == null) return new Result(Failure.MISSING_PREREQ, 0);
        if (data.hasMagicNode(node.id())) return new Result(Failure.ALREADY_UNLOCKED, node.cost());
        for (String p : node.prerequisites()) {
            if (MagicTreeCatalog.LOCKED_SENTINEL.equals(p)) {
                return new Result(Failure.LOCKED, node.cost());
            }
            if (!data.hasMagicNode(p)) {
                return new Result(Failure.MISSING_PREREQ, node.cost());
            }
        }
        int adjusted = affinityAdjustedCost(data, node.branch(), node.cost());
        int available = switch (node.currency()) {
            case ARCANE -> data.getArcanePoints();
            case SCHOOL -> data.getSchoolPoints(node.branch());
        };
        if (available < adjusted) return new Result(Failure.NOT_ENOUGH_POINTS, adjusted);
        return new Result(Failure.NONE, adjusted);
    }

    public static int affinityAdjustedCost(PlayerStatData data, MagicBranch branch, int baseCost) {
        if (baseCost <= 0 || branch == null || branch == MagicBranch.COMMON) return baseCost;
        MagicRace race = data == null ? null : data.getMagicRace();
        if (race == null) return baseCost * 2;
        if (!race.hasAffinity(branch)) return baseCost * 2;
        MagicBranch start = data.getChosenStartBranch();
        if (start != null && start != branch) {
            return Math.max(1, (baseCost + 1) / 2);
        }
        return baseCost;
    }
}
