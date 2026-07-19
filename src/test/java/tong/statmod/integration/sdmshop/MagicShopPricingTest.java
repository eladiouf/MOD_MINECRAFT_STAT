package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MagicShopPricingTest {
    @Test
    void appliesEveryRarityBaseAtLevelOne() {
        assertEquals(500, MagicShopPricing.scrollPrice("common", 1));
        assertEquals(1_200, MagicShopPricing.scrollPrice("uncommon", 1));
        assertEquals(3_000, MagicShopPricing.scrollPrice("rare", 1));
        assertEquals(7_500, MagicShopPricing.scrollPrice("epic", 1));
        assertEquals(20_000, MagicShopPricing.scrollPrice("legendary", 1));
    }

    @Test
    void appliesTheApprovedLevelMultipliers() {
        long[] expected = {500, 1_000, 2_000, 3_500, 5_500, 8_000, 11_000, 14_500, 18_500, 23_000};
        for (int level = 1; level <= expected.length; level++) {
            assertEquals(expected[level - 1], MagicShopPricing.scrollPrice("common", level));
        }
    }

    @Test
    void fallsBackToCommonForUnknownOrNullRarity() {
        assertEquals(2_000, MagicShopPricing.scrollPrice("mythic", 3));
        assertEquals(2_000, MagicShopPricing.scrollPrice(null, 3));
    }

    @Test
    void extendsLevelsAboveTenWithQuadraticMultiplier() {
        assertEquals(30_500, MagicShopPricing.scrollPrice("common", 11));
        assertEquals(100_000, MagicShopPricing.scrollPrice("common", 20));
    }

    @Test
    void rejectsNonPositiveLevels() {
        assertThrows(IllegalArgumentException.class, () -> MagicShopPricing.scrollPrice("common", 0));
    }
}
