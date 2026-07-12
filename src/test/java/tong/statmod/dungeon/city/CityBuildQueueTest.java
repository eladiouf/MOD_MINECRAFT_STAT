package tong.statmod.dungeon.city;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CityBuildQueueTest {

    @Test
    void tickRespectsBudgetAndOrder() {
        CityBuildQueue q = new CityBuildQueue();
        StringBuilder order = new StringBuilder();
        for (char c : "abcde".toCharArray()) q.add(() -> order.append(c));
        assertEquals(2, q.tick(2));
        assertEquals("ab", order.toString());
        assertEquals(3, q.tick(99));
        assertEquals("abcde", order.toString());
        assertEquals(0, q.tick(5), "file vide → 0 job exécuté");
    }

    @Test
    void progressPercentIsMonotonic() {
        CityBuildQueue q = new CityBuildQueue();
        AtomicInteger runs = new AtomicInteger();
        for (int i = 0; i < 4; i++) q.add(runs::incrementAndGet);
        assertEquals(0, q.progressPercent());
        q.tick(1);
        assertEquals(25, q.progressPercent());
        q.tick(3);
        assertEquals(100, q.progressPercent());
        assertTrue(q.isDone());
        assertEquals(4, runs.get());
    }

    @Test
    void emptyQueueIsDoneAt100Percent() {
        CityBuildQueue q = new CityBuildQueue();
        assertTrue(q.isDone());
        assertEquals(100, q.progressPercent());
    }

    @Test
    void clearResetsEverything() {
        CityBuildQueue q = new CityBuildQueue();
        q.add(() -> {});
        q.tick(1);
        q.clear();
        assertTrue(q.isDone());
        assertEquals(0, q.totalJobs());
        assertEquals(0, q.completedJobs());
    }

    /** Un job qui lève ne bloque pas définitivement la file (log + on continue). */
    @Test
    void throwingJobDoesNotStallTheQueue() {
        CityBuildQueue q = new CityBuildQueue();
        AtomicInteger after = new AtomicInteger();
        q.add(() -> { throw new IllegalStateException("boom"); });
        q.add(after::incrementAndGet);
        assertEquals(2, q.tick(2));
        assertEquals(1, after.get());
        assertTrue(q.isDone());
    }
}
