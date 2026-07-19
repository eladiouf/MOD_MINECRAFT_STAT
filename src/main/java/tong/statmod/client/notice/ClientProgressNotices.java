package tong.statmod.client.notice;

import java.util.List;
import tong.statmod.network.StatProgressNoticeMessage;

public final class ClientProgressNotices {
    private static final ProgressNoticeQueue QUEUE = new ProgressNoticeQueue();
    private static long tick;

    private ClientProgressNotices() {
    }

    public static void offer(StatProgressNoticeMessage message) {
        QUEUE.offer(message, tick);
    }

    public static void tick() {
        tick++;
        QUEUE.tick();
    }

    public static List<ProgressNotice> snapshot() {
        return QUEUE.snapshot();
    }

    public static void clear() {
        QUEUE.clear();
        tick = 0;
    }
}
