package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonScalingHandlerTest {
    @Test
    void summonHealthAndDamageScaleWithStats() {
        assertEquals(1.0f, SummonScalingHandler.healthMultiplier(0), 0.0001f);
        assertEquals(1.5f, SummonScalingHandler.healthMultiplier(50), 0.0001f);
        assertEquals(1.0f, SummonScalingHandler.damageMultiplier(0), 0.0001f);
        assertTrue(SummonScalingHandler.damageMultiplier(40) > 1.0f);
    }
}
