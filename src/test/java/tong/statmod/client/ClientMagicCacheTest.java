package tong.statmod.client;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientMagicCacheTest {
    @Test
    void updateClampsInvalidValuesAndClearsMalformedMasteryPayloads() {
        ClientMagicCache.reset();
        ClientMagicCache.update(
                new String[]{null, "fire/t1"},
                new String[]{null, "spell:one"},
                -4,
                new int[]{-7},
                999,
                -5);

        assertFalse(ClientMagicCache.hasMagicNode(null));
        assertEquals(1, ClientMagicCache.getMagicNodeCount());
        assertEquals(1, ClientMagicCache.getLearnedSpellsCount());
        assertEquals(0, ClientMagicCache.getMagicPoints());
        assertEquals(0, ClientMagicCache.getMasteryProgress(MagicBranch.FIRE));
        assertEquals(-1, ClientMagicCache.getRaceOrdinal());
        assertEquals(-1, ClientMagicCache.getStartBranchOrdinal());
    }
}
