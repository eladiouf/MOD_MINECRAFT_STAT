package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PhysicalEnduranceScalingTest {
    @Test
    void isNeutralAtZero() {
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(0, 1.0));
    }

    @Test
    void reachesConfiguredBonusAtOneHundred() {
        assertEquals(1.0, PhysicalEnduranceScaling.bonus(100, 1.0));
    }

    @Test
    void scalesLinearly() {
        assertEquals(0.25, PhysicalEnduranceScaling.bonus(50, 0.5));
    }

    @Test
    void clampsLevel() {
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(-1, 1.0));
        assertEquals(1.0, PhysicalEnduranceScaling.bonus(101, 1.0));
    }

    @Test
    void rejectsUnsafeConfiguredAmounts() {
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(100, -1.0));
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(100, Double.NaN));
        assertEquals(0.0, PhysicalEnduranceScaling.bonus(100, Double.POSITIVE_INFINITY));
    }
}
