package tong.statmod.client.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tong.statmod.network.StatProgressNoticeMessage;
import tong.statmod.stats.StatType;

class ProgressNoticeQueueTest {
    @Test
    void mergesOnlyWithinWindowForSameStat() {
        ProgressNoticeQueue queue = new ProgressNoticeQueue();
        queue.offer(message(StatType.AGILITY, 5, 2, 0), 10);
        queue.offer(message(StatType.AGILITY, 7, 2, 0), 29);

        assertEquals(1, queue.snapshot().size());
        assertEquals(12, queue.snapshot().get(0).awardedXp());

        queue.offer(message(StatType.AGILITY, 3, 2, 0), 50);
        assertEquals(2, queue.snapshot().size());
    }

    @Test
    void keepsAtMostFourNewestNotices() {
        ProgressNoticeQueue queue = new ProgressNoticeQueue();
        StatType[] stats = {
                StatType.AGILITY, StatType.COOKING, StatType.FORGING,
                StatType.PRECISION, StatType.TRACKING
        };
        for (int index = 0; index < stats.length; index++) {
            queue.offer(message(stats[index], 1, 1, 0), index * 30L);
        }

        assertEquals(4, queue.snapshot().size());
        assertEquals(StatType.COOKING, queue.snapshot().get(0).stat());
    }

    @Test
    void durationsFadeAndClearAreBounded() {
        ProgressNoticeQueue xp = new ProgressNoticeQueue();
        xp.offer(message(StatType.AGILITY, 5, 2, 0), 0);
        tick(xp, ProgressNoticeQueue.XP_DURATION - ProgressNoticeQueue.FADE_TICKS);
        assertEquals(1.0F, xp.snapshot().get(0).alpha());
        tick(xp, ProgressNoticeQueue.FADE_TICKS);
        assertTrue(xp.snapshot().isEmpty());

        ProgressNoticeQueue level = new ProgressNoticeQueue();
        level.offer(message(StatType.AGILITY, 5, 3, 1), 0);
        tick(level, ProgressNoticeQueue.LEVEL_DURATION - 1);
        assertTrue(level.snapshot().get(0).alpha() > 0.0F);
        level.tick();
        assertTrue(level.snapshot().isEmpty());

        level.offer(message(StatType.COOKING, 2, 1, 0), 100);
        level.clear();
        assertTrue(level.snapshot().isEmpty());
    }

    private static StatProgressNoticeMessage message(
            StatType stat, int xp, int level, int gained) {
        return new StatProgressNoticeMessage(stat, xp, level, gained);
    }

    private static void tick(ProgressNoticeQueue queue, int count) {
        for (int index = 0; index < count; index++) {
            queue.tick();
        }
    }
}
