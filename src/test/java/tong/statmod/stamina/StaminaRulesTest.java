package tong.statmod.stamina;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaminaRulesTest {
    @Test
    void maxStaminaUsesBasePlusEnduranceScaling() {
        assertEquals(100.0f, StaminaRules.maxStamina(0));
        assertEquals(150.0f, StaminaRules.maxStamina(50));
    }

    @Test
    void thresholdUsesRatioBands() {
        assertEquals(StaminaThreshold.NORMAL, StaminaRules.threshold(90.0f, 100.0f));
        assertEquals(StaminaThreshold.LOW, StaminaRules.threshold(30.0f, 100.0f));
        assertEquals(StaminaThreshold.CRITICAL, StaminaRules.threshold(10.0f, 100.0f));
    }
}
