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
        data.setCurrentStamina(320.0f);

        StaminaManager.restore(data, 30.0f, 10);
        assertEquals(330.0f, data.currentStamina(), 0.0001f);
    }

    @Test
    void idlePassiveTickDoesNotRegenerateOrDrainReserve() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(50.0f);

        StaminaManager.tickPassive(data, 10, false, false);
        assertEquals(50.0f, data.currentStamina(), 0.0001f);
    }

    @Test
    void passiveTickClampsCurrentStaminaWhenDerivedMaxDrops() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(StaminaRules.maxStamina(50));

        StaminaManager.tickPassive(data, 0, false, false);

        assertEquals(StaminaRules.maxStamina(0), data.currentStamina(), 0.0001f);
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
    void sprintDrainIsSustainableForLongCombatMovement() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(StaminaRules.maxStamina(0));

        for (int tick = 0; tick < 200; tick++) {
            StaminaManager.tickPassive(data, 0, false, false, true, false);
        }

        assertTrue(data.currentStamina() >= StaminaRules.maxStamina(0) * 0.90f);
    }

    @Test
    void spendingStaminaBuildsFatigueDebt() {
        StaminaData data = new StaminaData();

        assertTrue(StaminaManager.consume(data, 20.0f));
        assertTrue(data.fatigueDebt() > 0.0f);
    }

    @Test
    void staminaDataRejectsNonFiniteValues() {
        StaminaData data = new StaminaData();

        data.setCurrentStamina(Float.NaN);
        data.setFatigueDebt(Float.POSITIVE_INFINITY);

        assertEquals(0.0f, data.currentStamina(), 0.0001f);
        assertEquals(0.0f, data.fatigueDebt(), 0.0001f);
    }

    @Test
    void nonFiniteThresholdInputsAreCritical() {
        assertEquals(StaminaThreshold.CRITICAL,
                StaminaRules.threshold(Float.NaN, StaminaRules.maxStamina(0)));
        assertEquals(StaminaThreshold.CRITICAL,
                StaminaRules.threshold(50.0f, Float.POSITIVE_INFINITY));
    }

    @Test
    void consumeRejectsNonFiniteAmountsWithoutChangingPool() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(100.0f);

        assertFalse(StaminaManager.consume(data, Float.NaN));
        assertFalse(StaminaManager.consume(data, Float.POSITIVE_INFINITY));
        assertEquals(100.0f, data.currentStamina(), 0.0001f);
        assertEquals(0.0f, data.fatigueDebt(), 0.0001f);
    }

    @Test
    void meditationPaysDownFatigueDebt() {
        StaminaData data = new StaminaData();
        data.setFatigueDebt(10.0f);

        StaminaManager.tickPassive(data, 10, true, false, false, false);
        assertTrue(data.fatigueDebt() < 10.0f);
    }

    @Test
    void fatigueDebtSlowsMeditationRecovery() {
        StaminaData fresh = new StaminaData();
        fresh.setCurrentStamina(40.0f);
        StaminaData tired = new StaminaData();
        tired.setCurrentStamina(40.0f);
        tired.setFatigueDebt(80.0f);

        StaminaManager.tickPassive(fresh, 10, true, false);
        StaminaManager.tickPassive(tired, 10, true, false);

        assertTrue(fresh.currentStamina() > tired.currentStamina());
    }

    @Test
    void staminaDataCanBeCopiedForRespawnPersistence() {
        StaminaData source = new StaminaData();
        source.setCurrentStamina(123.0f);
        source.setFatigueDebt(7.0f);
        source.setMeditating(true);

        StaminaData target = new StaminaData();
        target.copyFrom(source);

        assertEquals(123.0f, target.currentStamina(), 0.0001f);
        assertEquals(7.0f, target.fatigueDebt(), 0.0001f);
        assertTrue(target.meditating());
    }
}
