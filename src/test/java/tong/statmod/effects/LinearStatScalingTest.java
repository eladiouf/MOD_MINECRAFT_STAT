package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LinearStatScalingTest {
    @Test
    void isNeutralAtZero() {
        assertEquals(0.0, LinearStatScaling.bonus(0, 1.0));
    }

    @Test
    void scalesLinearlyToConfiguredMaximum() {
        assertEquals(0.25, LinearStatScaling.bonus(50, 0.5));
        assertEquals(0.5, LinearStatScaling.bonus(100, 0.5));
    }

    @Test
    void clampsLevel() {
        assertEquals(0.0, LinearStatScaling.bonus(-1, 1.0));
        assertEquals(1.0, LinearStatScaling.bonus(101, 1.0));
    }

    @Test
    void rejectsUnsafeConfiguredAmounts() {
        assertEquals(0.0, LinearStatScaling.bonus(100, -1.0));
        assertEquals(0.0, LinearStatScaling.bonus(100, Double.NaN));
        assertEquals(0.0, LinearStatScaling.bonus(100, Double.POSITIVE_INFINITY));
    }
}
