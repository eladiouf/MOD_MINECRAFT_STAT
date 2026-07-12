package tong.statmod.client;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientMagicCacheTest {
    @Test
    void update_exposes_practice_mastery_separately_from_bankable_mastery() {
        int[] bankable = new int[MagicBranch.values().length];
        int[] practice = new int[MagicBranch.values().length];
        bankable[MagicBranch.FIRE.ordinal()] = 125;
        practice[MagicBranch.FIRE.ordinal()] = 9;

        ClientMagicCache.update(new String[0], new String[0], 0, bankable, practice, -1, -1);

        assertEquals(125, ClientMagicCache.getMasteryProgress(MagicBranch.FIRE));
        assertEquals(9, ClientMagicCache.getPracticeMasteryProgress(MagicBranch.FIRE));
    }

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
