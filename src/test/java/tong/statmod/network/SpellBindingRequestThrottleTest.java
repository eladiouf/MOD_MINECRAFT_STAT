package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SpellBindingRequestThrottleTest {
    @AfterEach
    void reset() {
        SpellBindingRequestThrottle.clearAll();
    }

    @Test
    void allowsFirstRequestThenWaitsTenTicks() {
        UUID player = UUID.randomUUID();

        assertTrue(SpellBindingRequestThrottle.allow(player, 100));
        assertFalse(SpellBindingRequestThrottle.allow(player, 109));
        assertTrue(SpellBindingRequestThrottle.allow(player, 110));
        SpellBindingRequestThrottle.clear(player);
        assertTrue(SpellBindingRequestThrottle.allow(player, 110));
    }

    @Test
    void rejectsNullPlayersAndHandlesClockRollback() {
        assertFalse(SpellBindingRequestThrottle.allow(null, 10));
        UUID player = UUID.randomUUID();
        assertTrue(SpellBindingRequestThrottle.allow(player, 100));
        assertTrue(SpellBindingRequestThrottle.allow(player, 20));
    }
}
