package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagiculeScalingHandlerTest {
    @Test
    void computesMagiculeFloorFromRelevantStats() {
        assertEquals(0.0, MagiculeScalingHandler.magiculeFloor(0, 0), 0.0001);
        assertEquals(75.0, MagiculeScalingHandler.magiculeFloor(50, 25), 0.0001);
    }

    @Test
    void raisesBaseMaxMagiculeToMatchTheStatFloor() {
        assertEquals(120.0, MagiculeScalingHandler.requiredMaxMagicule(20.0, 100, 20), 0.0001);
        assertEquals(400.0, MagiculeScalingHandler.requiredMaxMagicule(400.0, 10, 10), 0.0001);
    }

    @Test
    void scalesCurrentMagiculeWithoutExceedingEffectiveMax() {
        assertEquals(120.0, MagiculeScalingHandler.scaledMagicule(20.0, 200.0, 100, 20), 0.0001);
        assertEquals(50.0, MagiculeScalingHandler.scaledMagicule(20.0, 50.0, 100, 20), 0.0001);
        assertTrue(MagiculeScalingHandler.scaledMagicule(400.0, 50.0, 10, 10) >= 400.0);
    }
}
