package tong.statmod.event;

public final class BookStudyTimeline {
    public static final long STUDY_TICKS = 40L;
    public static final long HEARTBEAT_TIMEOUT_TICKS = 8L;

    private BookStudyTimeline() {
    }

    public static boolean complete(long startTick, long now) {
        return now >= startTick && now - startTick >= STUDY_TICKS;
    }

    public static boolean timedOut(long heartbeatTick, long now) {
        return now < heartbeatTick || now - heartbeatTick > HEARTBEAT_TIMEOUT_TICKS;
    }
}
