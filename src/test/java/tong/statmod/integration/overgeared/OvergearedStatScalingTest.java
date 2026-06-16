package tong.statmod.integration.overgeared;

import net.stirdrem.overgeared.ForgingQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OvergearedStatScalingTest {
    @Test
    void qualityMultiplierMatchesForgingTier() {
        assertEquals(1.0, OvergearedStatScaling.xpMultiplier(ForgingQuality.POOR), 0.0001);
        assertEquals(1.2, OvergearedStatScaling.xpMultiplier(ForgingQuality.WELL), 0.0001);
        assertEquals(1.4, OvergearedStatScaling.xpMultiplier(ForgingQuality.EXPERT), 0.0001);
        assertEquals(1.6, OvergearedStatScaling.xpMultiplier(ForgingQuality.PERFECT), 0.0001);
        assertEquals(1.8, OvergearedStatScaling.xpMultiplier(ForgingQuality.MASTER), 0.0001);
    }

    @Test
    void scaledXpRoundsToNearestInteger() {
        assertEquals(9, OvergearedStatScaling.scaledXp(5, ForgingQuality.MASTER));
        assertEquals(5, OvergearedStatScaling.scaledXp(5, ForgingQuality.NONE));
    }

    @Test
    void digSpeedScalesWithForgingLevel() {
        assertEquals(1.0f, OvergearedStatScaling.digSpeedMultiplier(0), 0.0001f);
        assertEquals(2.0f, OvergearedStatScaling.digSpeedMultiplier(50), 0.0001f);
    }

    @Test
    void durabilityDamageShrinksWithForgingLevel() {
        assertEquals(4, OvergearedStatScaling.adjustedDurabilityDamage(4, 0));
        assertEquals(3, OvergearedStatScaling.adjustedDurabilityDamage(4, 2));
        assertEquals(0, OvergearedStatScaling.adjustedDurabilityDamage(4, 10));
        assertEquals(0, OvergearedStatScaling.adjustedDurabilityDamage(0, 50));
    }
}
