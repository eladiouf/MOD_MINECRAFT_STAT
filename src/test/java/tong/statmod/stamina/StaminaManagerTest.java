package tong.statmod.stamina;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaminaManagerTest {
    @Test
    void consumeReturnsFalseWhenAmountExceedsCurrentPool() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(20.0f);

        assertFalse(StaminaManager.consume(data, 30.0f));
        assertEquals(20.0f, data.currentStamina(), 0.0001f);
    }

    @Test
    void restoreClampsToDerivedMax() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(90.0f);

        StaminaManager.restore(data, 30.0f, 10);
        assertEquals(110.0f, data.currentStamina(), 0.0001f);
    }

    @Test
    void passiveTickRegeneratesWhenBelowMax() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(50.0f);

        StaminaManager.tickPassive(data, 10, false, false);
        assertTrue(data.currentStamina() > 50.0f);
    }

    @Test
    void meditationRecoveryBeatsPassiveRecovery() {
        float passive = StaminaRules.passiveRecoveryPerTick(false);
        float meditation = StaminaRules.passiveRecoveryPerTick(true);

        assertTrue(meditation > passive);
    }
}
