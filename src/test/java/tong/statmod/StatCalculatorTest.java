package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatCalculator;
import static org.junit.jupiter.api.Assertions.*;

class StatCalculatorTest {

    @Test
    void damageBonus_nonNegative() {
        float bonus = StatCalculator.getDamageBonus(50);
        assertTrue(bonus > 0);
        assertTrue(bonus < 1.0f);
    }

    @Test
    void damageReduction_neverExceeds100Percent() {
        float reduction = StatCalculator.getDamageReduction(100);
        assertTrue(reduction > 0);
        assertTrue(reduction < 1.0f);
    }

    @Test
    void enduranceHearts_nonNegative() {
        float hearts = StatCalculator.getEnduranceHearts(0);
        assertEquals(0.0f, hearts, 0.001f);

        hearts = StatCalculator.getEnduranceHearts(50);
        assertTrue(hearts > 0);
    }

    @Test
    void critChance_range() {
        float crit = StatCalculator.getCritChance(100);
        assertTrue(crit >= 0.0f);
        assertTrue(crit <= 1.0f);
    }

    @Test
    void manaBonus_linear() {
        assertEquals(0, StatCalculator.getManaBonus(0));
        assertEquals(50, StatCalculator.getManaBonus(50));
        assertEquals(100, StatCalculator.getManaBonus(100));
    }

    @Test
    void damageBonus_maxAtLevel100() {
        float bonus = StatCalculator.getDamageBonus(100);
        assertTrue(bonus > 0);
        assertTrue(bonus <= 0.5f);
    }

}
