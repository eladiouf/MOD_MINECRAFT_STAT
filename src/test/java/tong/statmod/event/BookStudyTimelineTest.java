package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BookStudyTimelineTest {
    @Test
    void completesOnlyAfterFortyTicks() {
        assertFalse(BookStudyTimeline.complete(100, 139));
        assertTrue(BookStudyTimeline.complete(100, 140));
        assertFalse(BookStudyTimeline.complete(100, 99));
    }

    @Test
    void heartbeatExpiresOnlyAfterEightTicks() {
        assertFalse(BookStudyTimeline.timedOut(100, 108));
        assertTrue(BookStudyTimeline.timedOut(100, 109));
        assertTrue(BookStudyTimeline.timedOut(100, 99));
    }
}
