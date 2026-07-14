package tong.statmod.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StatTypeTest {
    @Test
    void exposesTwentyThreeUniqueStableIdsInSixFamilies() {
        Set<String> ids = Arrays.stream(StatType.values())
                .map(StatType::id)
                .collect(Collectors.toSet());
        Set<StatFamily> families = Arrays.stream(StatType.values())
                .map(StatType::family)
                .collect(Collectors.toSet());

        assertEquals(23, StatType.values().length);
        assertEquals(23, ids.size());
        assertEquals(6, families.size());
    }

    @Test
    void resolvesIdsWithoutUsingOrdinals() {
        assertEquals(StatType.ARCANE_POWER, StatType.fromId("arcane_power").orElseThrow());
        assertTrue(StatType.fromId("ARCANE_POWER").isEmpty());
        assertTrue(StatType.fromId("removed_stat").isEmpty());
    }
}
