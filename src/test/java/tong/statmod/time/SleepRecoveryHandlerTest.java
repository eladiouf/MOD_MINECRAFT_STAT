package tong.statmod.time;

import org.junit.jupiter.api.Test;
import tong.statmod.stamina.StaminaData;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepRecoveryHandlerTest {
    @Test
    void wakeBonusRestoresMoreThanPassiveSleepTick() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(10.0f);

        SleepRecoveryHandler.applyWakeBonus(data, 50);
        assertTrue(data.currentStamina() > 30.0f);
    }

    @Test
    void wakeBonusAlsoClearsPartOfFatigueDebt() {
        StaminaData data = new StaminaData();
        data.setFatigueDebt(20.0f);

        SleepRecoveryHandler.applyWakeBonus(data, 50);
        assertTrue(data.fatigueDebt() < 20.0f);
    }
}
