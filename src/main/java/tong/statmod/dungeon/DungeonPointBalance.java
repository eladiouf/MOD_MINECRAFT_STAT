package tong.statmod.dungeon;

/** Pure depth-aware economy policy for Trial Dungeon points. */
public final class DungeonPointBalance {
    private static final double POINTS_PER_DIFFICULTY = 0.18;
    private static final double DEPTH_RATE_PER_FLOOR = 0.005;
    private static final int DEPTH_MULTIPLIER_CAP_FLOOR = 150;
    private static final int MOB_CAP_MAX = 600;
    private static final double ASSIST_SHARE = 0.40;

    private DungeonPointBalance() {}

    public static int mobReward(double difficulty, int floor) {
        int depth = safeFloor(floor);
        double safeDifficulty = Double.isFinite(difficulty) ? Math.max(0.0, difficulty) : 0.0;
        double depthMultiplier = 1.0
                + Math.min(depth, DEPTH_MULTIPLIER_CAP_FLOOR) * DEPTH_RATE_PER_FLOOR;
        int minimum = saturatedInt(3L + depth / 25L);
        int cap = saturatedInt(Math.min(MOB_CAP_MAX, 120L + 3L * depth));
        long raw = Math.round(safeDifficulty * POINTS_PER_DIFFICULTY * depthMultiplier);
        return (int) Math.max(minimum, Math.min((long) cap, raw));
    }

    public static int floorClearReward(int floor) {
        int depth = safeFloor(floor);
        return saturatedInt(100L + 18L * depth + 60L * (depth / 10L));
    }

    public static int bossReward(int floor) {
        int depth = safeFloor(floor);
        return saturatedInt(750L + 80L * depth + 100L * (depth / 10L));
    }

    public static int assistShare(int basePoints) {
        if (basePoints <= 0) return 0;
        return (int) Math.floor(basePoints * ASSIST_SHARE);
    }

    public static int deathLoss(int currentPoints, int floor) {
        if (currentPoints <= 0) return 0;
        int depth = safeFloor(floor);
        int riskBasisPoints = 1_000 + Math.min(depth, 150) * 10;
        long proportional = ((long) currentPoints * riskBasisPoints + 9_999L) / 10_000L;
        long minimum = 50L + 12L * depth;
        return (int) Math.min((long) currentPoints, Math.max(minimum, proportional));
    }

    private static int safeFloor(int floor) {
        return Math.max(1, floor);
    }

    private static int saturatedInt(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }
}
