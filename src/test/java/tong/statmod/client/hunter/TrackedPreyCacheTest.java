package tong.statmod.client.hunter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tong.statmod.network.TrackedPreyMessage;

class TrackedPreyCacheTest {
    @Test
    void replacesRefreshesExpiresAndClearsOneMark() {
        TrackedPreyCache cache = new TrackedPreyCache();
        cache.accept(new TrackedPreyMessage(7, 20), 100);
        assertEquals(7, cache.entityId(119).orElseThrow());
        assertTrue(cache.entityId(120).isEmpty());

        cache.accept(new TrackedPreyMessage(8, 40), 200);
        cache.accept(new TrackedPreyMessage(9, 40), 210);
        assertEquals(9, cache.entityId(249).orElseThrow());
        cache.accept(TrackedPreyMessage.clear(), 220);
        assertTrue(cache.entityId(220).isEmpty());
    }

    @Test
    void saturatesExpiryNearTheLongLimit() {
        TrackedPreyCache cache = new TrackedPreyCache();
        cache.accept(new TrackedPreyMessage(4, 20), Long.MAX_VALUE - 10);

        assertEquals(4, cache.entityId(Long.MAX_VALUE - 1).orElseThrow());
        assertTrue(cache.entityId(Long.MAX_VALUE).isEmpty());
    }
}
