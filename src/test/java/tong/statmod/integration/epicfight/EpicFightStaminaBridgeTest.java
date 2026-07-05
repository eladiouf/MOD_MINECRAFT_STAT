package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;
import tong.statmod.stamina.StaminaThreshold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightStaminaBridgeTest {
    @Test
    void criticalThresholdBlocksSkillUse() {
        assertFalse(EpicFightStaminaBridge.canUseSkill(StaminaThreshold.CRITICAL, 4.0f, 5.0f));
        assertTrue(EpicFightStaminaBridge.canUseSkill(StaminaThreshold.NORMAL, 20.0f, 15.0f));
    }

    @Test
    void criticalThresholdStillAllowsSkillsWhenLargePoolCanPayCost() {
        assertTrue(EpicFightStaminaBridge.canUseSkill(StaminaThreshold.CRITICAL, 20.0f, 5.0f));
    }

    @Test
    void lowThresholdAppliesDamagePenalty() {
        assertEquals(0.9f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.LOW), 0.0001f);
        assertEquals(1.0f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.NORMAL), 0.0001f);
    }

    @Test
    void skillCostIsCappedAgainstStatModPoolForLongCombat() {
        assertEquals(12.0f, EpicFightStaminaBridge.sustainableSkillCost(100.0f, 1.0f, 300.0f), 0.0001f);
        assertEquals(2.5f, EpicFightStaminaBridge.sustainableSkillCost(5.0f, 0.5f, 300.0f), 0.0001f);
    }
}
