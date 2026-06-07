package tong.statmod.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobStatCalculatorTest {

    @Test
    void getDamageBonusMultiplier_zeroAtLevelZero() {
        assertEquals(0.0f, MobStatCalculator.getDamageBonusMultiplier(0), 0.001f);
    }

    @Test
    void getDamageBonusMultiplier_twoAtLevelHundred() {
        assertEquals(2.0f, MobStatCalculator.getDamageBonusMultiplier(100), 0.001f);
    }

    @Test
    void getHealthBonusFlat_zeroAtLevelZero() {
        assertEquals(0.0f, MobStatCalculator.getHealthBonusFlat(0), 0.001f);
    }

    @Test
    void getHealthBonusFlat_fiftyAtLevelHundred() {
        assertEquals(50.0f, MobStatCalculator.getHealthBonusFlat(100), 0.001f);
    }

    @Test
    void getSpeedBonus_zeroAtLevelZero() {
        assertEquals(0.0f, MobStatCalculator.getSpeedBonus(0), 0.001f);
    }

    @Test
    void getDamageReduction_cappedAtHalf() {
        float reduction = MobStatCalculator.getDamageReduction(100);
        assertTrue(reduction <= 0.5f, "Damage reduction should not exceed 50%");
    }

    @Test
    void getFollowRangeBonus_growsWithLevel() {
        float low  = MobStatCalculator.getFollowRangeBonus(10);
        float high = MobStatCalculator.getFollowRangeBonus(50);
        assertTrue(high > low);
    }

    @Test
    void getKnockbackResistance_cappedAtOne() {
        assertEquals(1.0f, MobStatCalculator.getKnockbackResistance(200), 0.001f);
    }

    @Test
    void getElementalResistance_cappedAtHalf() {
        assertEquals(0.5f, MobStatCalculator.getElementalResistance(200), 0.001f);
    }
}
