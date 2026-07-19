package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatModRuntimeTest {
    @Test
    void exposesStableProtocolAndThirdVersionedPlayerSchema() {
        assertEquals("9", StatModRuntime.NETWORK_PROTOCOL);
        assertEquals(3, StatModRuntime.PLAYER_STATS_SCHEMA);
    }
}
