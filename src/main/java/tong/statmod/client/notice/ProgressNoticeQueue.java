package tong.statmod.client.notice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import tong.statmod.network.StatProgressNoticeMessage;

public final class ProgressNoticeQueue {
    public static final int MAX_NOTICES = 4;
    public static final int MERGE_WINDOW = 20;
    public static final int XP_DURATION = 50;
    public static final int LEVEL_DURATION = 80;
    public static final int FADE_TICKS = 15;

    private final Deque<ProgressNotice> notices = new ArrayDeque<>();

    public void offer(StatProgressNoticeMessage message, long tick) {
        if (!message.valid() || message.levelsGained() <= 0) {
            return;
        }
        ProgressNotice newest = notices.peekLast();
        if (newest != null
                && newest.stat() == message.stat()
                && tick - newest.updatedAt() < MERGE_WINDOW) {
            notices.removeLast();
            notices.addLast(newest.merge(message, tick));
            return;
        }
        notices.addLast(new ProgressNotice(
                message.stat(), message.awardedXp(), message.newLevel(),
                message.levelsGained(), tick, durationFor(message.levelsGained())));
        while (notices.size() > MAX_NOTICES) {
            notices.removeFirst();
        }
    }

    public void tick() {
        int count = notices.size();
        for (int index = 0; index < count; index++) {
            ProgressNotice notice = notices.removeFirst().nextTick();
            if (notice.remainingTicks() > 0) {
                notices.addLast(notice);
            }
        }
    }

    public List<ProgressNotice> snapshot() {
        return List.copyOf(new ArrayList<>(notices));
    }

    public void clear() {
        notices.clear();
    }

    static int durationFor(int levelsGained) {
        return levelsGained > 0 ? LEVEL_DURATION : XP_DURATION;
    }
}
