package tong.statmod.integration.overgeared;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvergearedCompatTest {

    @Test
    void classifiesForgingBlocks() {
        assertTrue(OvergearedCompat.isOvergearedBlockId("overgeared:smithing_anvil"));
        assertTrue(OvergearedCompat.isOvergearedBlockId("overgeared:casting_furnace"));
        assertFalse(OvergearedCompat.isOvergearedBlockId("minecraft:anvil"));
    }

    @Test
    void mapsBlockKindsToStats() {
        assertEquals(StatType.FORGING, OvergearedCompat.statForBlockId("overgeared:smithing_anvil"));
        assertEquals(StatType.ALCHEMY, OvergearedCompat.statForBlockId("overgeared:casting_furnace"));
        assertEquals(StatType.COOKING, OvergearedCompat.statForBlockId("overgeared:alloy_furnace"));
    }
}
