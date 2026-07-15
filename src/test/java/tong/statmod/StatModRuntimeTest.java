package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatModRuntimeTest {
    @Test
    void exposesStableProtocolAndFirstVersionedPlayerSchema() {
        assertEquals("1", StatModRuntime.NETWORK_PROTOCOL);
        assertEquals(1, StatModRuntime.PLAYER_STATS_SCHEMA);
    }
}
