package tong.statmod.effects;

import tong.statmod.stats.StatProgress;

public final class HunterPerceptionRules {
    private static final double SCORE_PER_MILESTONE = 0.25;
    private static final double MAX_SCORE = 0.75;

    private HunterPerceptionRules() {
    }

    public static int milestoneCount(double normalizedScore) {
        double bounded = Double.isFinite(normalizedScore)
                ? Math.max(0.0, Math.min(MAX_SCORE, normalizedScore)) : 0.0;
        return Math.max(0, Math.min(3,
                (int) Math.round(bounded / SCORE_PER_MILESTONE)));
    }

    public static int trackingDurationTicks(int level, double normalizedScore) {
        int bounded = boundedLevel(level);
        return bounded == 0 ? 0 : 60 + bounded + 40 * milestoneCount(normalizedScore);
    }

    public static double trackingRangeBlocks(int level, double normalizedScore) {
        int bounded = boundedLevel(level);
        return bounded == 0 ? 0.0
                : 12.0 + 0.12 * bounded + 4.0 * milestoneCount(normalizedScore);
    }

    public static double keenSensesRangeBlocks(int level, double normalizedScore) {
        int bounded = boundedLevel(level);
        return bounded == 0 ? 0.0
                : 6.0 + 0.10 * bounded + 2.0 * milestoneCount(normalizedScore);
    }

    private static int boundedLevel(int level) {
        return Math.max(0, Math.min(StatProgress.MAX_LEVEL, level));
    }
}
