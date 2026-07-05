package tong.statmod.time;

import org.junit.jupiter.api.Test;
import tong.statmod.stamina.StaminaData;

import tong.statmod.stamina.StaminaRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepRecoveryHandlerTest {
    @Test
    void wakeBonusRestoresMoreThanPassiveSleepTick() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(10.0f);

        SleepRecoveryHandler.applyWakeBonus(data, 50);
        assertEquals(StaminaRules.maxStamina(50), data.currentStamina(), 0.0001f);
    }

    @Test
    void wakeBonusAlsoClearsPartOfFatigueDebt() {
        StaminaData data = new StaminaData();
        data.setFatigueDebt(20.0f);

        SleepRecoveryHandler.applyWakeBonus(data, 50);
        assertTrue(data.fatigueDebt() < 20.0f);
    }
}
