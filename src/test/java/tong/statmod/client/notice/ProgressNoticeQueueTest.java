package tong.statmod.client.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tong.statmod.network.StatProgressNoticeMessage;
import tong.statmod.stats.StatType;

class ProgressNoticeQueueTest {
    @Test
    void ignoresXpOnlyMessages() {
        ProgressNoticeQueue queue = new ProgressNoticeQueue();

        queue.offer(message(StatType.AGILITY, 5, 2, 0), 10);

        assertTrue(queue.snapshot().isEmpty());
    }

    @Test
    void mergesOnlyWithinWindowForSameStat() {
        ProgressNoticeQueue queue = new ProgressNoticeQueue();
        queue.offer(message(StatType.AGILITY, 5, 2, 1), 10);
        queue.offer(message(StatType.AGILITY, 7, 3, 1), 29);

        assertEquals(1, queue.snapshot().size());
        assertEquals(12, queue.snapshot().get(0).awardedXp());

        queue.offer(message(StatType.AGILITY, 3, 4, 1), 50);
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
            queue.offer(message(stats[index], 1, 1, 1), index * 30L);
        }

        assertEquals(4, queue.snapshot().size());
        assertEquals(StatType.COOKING, queue.snapshot().get(0).stat());
    }

    @Test
    void levelDurationFadeAndClearAreBounded() {
        assertEquals(60, ProgressNoticeQueue.LEVEL_DURATION);
        ProgressNoticeQueue level = new ProgressNoticeQueue();
        level.offer(message(StatType.AGILITY, 5, 3, 1), 0);
        tick(level, ProgressNoticeQueue.LEVEL_DURATION - 1);
        assertTrue(level.snapshot().get(0).alpha() > 0.0F);
        level.tick();
        assertTrue(level.snapshot().isEmpty());

        level.offer(message(StatType.COOKING, 2, 1, 1), 100);
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
