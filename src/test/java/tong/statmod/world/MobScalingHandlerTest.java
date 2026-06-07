package tong.statmod.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobScalingHandlerTest {

    @Test
    void computeHealthScale_isClampedAtMaxScale() {
        float maxScale = 2.5f;
        float result = MobScalingHandler.computeHealthScale(500, maxScale);
        assertEquals(maxScale, result, 0.001f, "Scale must be <= maxScale at high levels");
    }

    @Test
    void computeHealthScale_isOneWhenLevelTooLow() {
        float result = MobScalingHandler.computeHealthScale(5, 2.5f);
        assertEquals(1.0f, result, 0.001f, "Level <= 10 must give scale = 1.0");
    }

    @Test
    void computeHealthScale_growsWithLevel() {
        float low  = MobScalingHandler.computeHealthScale(20, 2.5f);
        float high = MobScalingHandler.computeHealthScale(50, 2.5f);
        assertTrue(high > low, "Higher level must give higher scale");
    }

    @Test
    void computeDamageScale_isClampedAtMaxScale() {
        float maxScale = 2.0f;
        float result = MobScalingHandler.computeDamageScale(500, maxScale);
        assertEquals(maxScale, result, 0.001f, "Damage scale must be capped");
    }
}
