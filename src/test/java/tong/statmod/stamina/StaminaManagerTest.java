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

    @Test
    void foodRecoveryScalesWithNutrition() {
        assertTrue(StaminaRules.foodRecoveryAmount(6, 0.6f) > StaminaRules.foodRecoveryAmount(2, 0.1f));
    }

    @Test
    void spendingStaminaBuildsFatigueDebt() {
        StaminaData data = new StaminaData();

        assertTrue(StaminaManager.consume(data, 20.0f));
        assertTrue(data.fatigueDebt() > 0.0f);
    }

    @Test
    void meditationPaysDownFatigueDebt() {
        StaminaData data = new StaminaData();
        data.setFatigueDebt(10.0f);

        StaminaManager.tickPassive(data, 10, true, false, false, false);
        assertTrue(data.fatigueDebt() < 10.0f);
    }

    @Test
    void fatigueDebtSlowsNaturalRecovery() {
        StaminaData fresh = new StaminaData();
        fresh.setCurrentStamina(40.0f);
        StaminaData tired = new StaminaData();
        tired.setCurrentStamina(40.0f);
        tired.setFatigueDebt(80.0f);

        StaminaManager.tickPassive(fresh, 10, false, false);
        StaminaManager.tickPassive(tired, 10, false, false);

        assertTrue(fresh.currentStamina() > tired.currentStamina());
    }
}
