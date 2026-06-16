package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;
import tong.statmod.stamina.StaminaThreshold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightStaminaBridgeTest {
    @Test
    void criticalThresholdBlocksSkillUse() {
        assertFalse(EpicFightStaminaBridge.canUseSkill(StaminaThreshold.CRITICAL, 20.0f, 15.0f));
        assertTrue(EpicFightStaminaBridge.canUseSkill(StaminaThreshold.NORMAL, 20.0f, 15.0f));
    }

    @Test
    void lowThresholdAppliesDamagePenalty() {
        assertEquals(0.9f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.LOW), 0.0001f);
        assertEquals(1.0f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.NORMAL), 0.0001f);
    }
}
