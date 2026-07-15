package tong.statmod.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

class ClientStatsCacheTest {
    @AfterEach
    void clear() {
        ClientStatsCache.clear();
    }

    @Test
    void replacePublishesOneImmutableRevision() {
        long before = ClientStatsCache.state().revision();

        ClientStatsCache.replace(Map.of(StatType.AGILITY, new StatValue(12, 34)));

        ClientStatsState state = ClientStatsCache.state();
        assertEquals(before + 1, state.revision());
        assertEquals(new StatValue(12, 34), state.values().get(StatType.AGILITY));
        assertEquals(StatType.values().length, state.values().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.values().put(StatType.AGILITY, new StatValue(0, 0)));
    }

    @Test
    void clearPublishesACompleteZeroSnapshot() {
        ClientStatsCache.replace(Map.of(StatType.AGILITY, new StatValue(12, 34)));

        ClientStatsCache.clear();

        assertEquals(StatType.values().length, ClientStatsCache.state().values().size());
        assertTrue(ClientStatsCache.state().values().values().stream()
                .allMatch(value -> value.equals(new StatValue(0, 0))));
    }
}
