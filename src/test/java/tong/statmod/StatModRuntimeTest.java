package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatModRuntimeTest {
    @Test
    void exposesStableProtocolAndSecondVersionedPlayerSchema() {
        assertEquals("2", StatModRuntime.NETWORK_PROTOCOL);
        assertEquals(2, StatModRuntime.PLAYER_STATS_SCHEMA);
    }
}
