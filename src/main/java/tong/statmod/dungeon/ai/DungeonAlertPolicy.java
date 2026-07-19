package tong.statmod.dungeon.ai;

public final class DungeonAlertPolicy {
    static final long MEMORY_TICKS = 100L;
    static final double SHARE_RANGE_SQUARED = 48.0 * 48.0;

    private DungeonAlertPolicy() {}

    public static DungeonAlertState next(DungeonAlertState current, boolean seesEnemy,
                                         boolean heardAlert, boolean hasRecentMemory,
                                         boolean lowHealth) {
        if (lowHealth) return DungeonAlertState.RETREATING;
        if (seesEnemy) return DungeonAlertState.COMBAT;
        if (heardAlert) return DungeonAlertState.ALERTED;
        if (hasRecentMemory) return DungeonAlertState.SUSPICIOUS;
        return DungeonAlertState.IDLE;
    }

    public static boolean canShare(long sourceTick, long nowTick, double distanceSquared) {
        long age = nowTick - sourceTick;
        return age >= 0L && age <= MEMORY_TICKS && distanceSquared <= SHARE_RANGE_SQUARED;
    }
}
