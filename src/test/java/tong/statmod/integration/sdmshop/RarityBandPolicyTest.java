package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RarityBandPolicyTest {
    private static final List<Double> WEIGHTS = List.of(0.3, 0.25, 0.2, 0.15, 0.1);

    @Test
    void reproducesIronRarityBandsForCommonSpells() {
        String[] expected = {"common", "common", "common", "uncommon", "uncommon",
                "rare", "rare", "epic", "epic", "legendary"};
        for (int level = 1; level <= 10; level++) {
            assertEquals(expected[level - 1],
                    RarityBandPolicy.rarity(0, 4, 10, level, WEIGHTS));
        }
    }

    @Test
    void renormalizesBandsWhenMinimumRarityIsHigher() {
        assertEquals("rare", RarityBandPolicy.rarity(2, 4, 5, 1, WEIGHTS));
        assertEquals("rare", RarityBandPolicy.rarity(2, 4, 5, 2, WEIGHTS));
        assertEquals("epic", RarityBandPolicy.rarity(2, 4, 5, 3, WEIGHTS));
        assertEquals("legendary", RarityBandPolicy.rarity(2, 4, 5, 4, WEIGHTS));
        assertEquals("legendary", RarityBandPolicy.rarity(2, 4, 5, 5, WEIGHTS));
    }

    @Test
    void singleLevelSpellKeepsItsMinimumRarity() {
        assertEquals("epic", RarityBandPolicy.rarity(3, 4, 1, 1, WEIGHTS));
    }
}
