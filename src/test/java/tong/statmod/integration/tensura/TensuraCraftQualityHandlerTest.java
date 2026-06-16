package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraCraftQualityHandlerTest {
    @Test
    void durabilityLossIsReducedByForgingStat() {
        assertEquals(10, TensuraCraftQualityHandler.reducedDurabilityLoss(10, 0));
        assertEquals(5, TensuraCraftQualityHandler.reducedDurabilityLoss(10, 50));
    }

    @Test
    void potionAndFoodBonusesScaleByRelevantStats() {
        assertTrue(TensuraCraftQualityHandler.potionDurationMultiplier(40) > 1.0f);
        assertTrue(TensuraCraftQualityHandler.foodSaturationBonus(30) > 0.0f);
    }
}
