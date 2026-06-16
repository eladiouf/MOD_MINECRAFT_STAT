package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.damagesource.StunType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightHandlersTest {
    @Test
    void executeThresholdTriggersBelowExpectedHealthRatio() {
        assertEquals(0.2f, EpicFightExecuteHandler.executeThreshold(50), 0.0001f);
        assertTrue(EpicFightExecuteHandler.shouldExecute(50, 0.05f));
        assertFalse(EpicFightExecuteHandler.shouldExecute(50, 0.25f));
    }

    @Test
    void cooldownMultiplierRespondsToResourceType() {
        assertTrue(EpicFightCooldownHandler.resourceMultiplier(40, 0, 0, Skill.Resource.COOLDOWN) < 1.0f);
        assertEquals(1.0f, EpicFightCooldownHandler.resourceMultiplier(40, 0, 0, Skill.Resource.HEALTH), 0.0001f);
    }

    @Test
    void stunResistanceAndHyperArmorScaleWithDefenseStats() {
        assertTrue(EpicFightStunResistanceHandler.stunTimeMultiplier(70, 50, StunType.SHORT) < 1.0f);
        assertTrue(EpicFightHyperArmorHandler.shouldNegateStun(50, 70, StunType.SHORT));
        assertFalse(EpicFightHyperArmorHandler.shouldNegateStun(50, 70, StunType.NEUTRALIZE));
    }

    @Test
    void reachBonusUsesPrecisionAndAgility() {
        assertTrue(EpicFightWeaponReachHandler.reachBonus(50, 40) > 0.0f);
    }

    @Test
    void airAttackBonusScalesWithAgility() {
        assertEquals(1.0f, EpicFightCompat.airAttackMultiplier(20, false), 0.0001f);
        assertEquals(1.1f, EpicFightCompat.airAttackMultiplier(20, true), 0.0001f);
    }

    @Test
    void weightAndImpactScalingFollowThePlanRatios() {
        assertEquals(-0.2d, EpicFightCompat.weightModifierAmount(10), 0.0001d);
        assertEquals(0.2d, EpicFightCompat.impactModifierAmount(10), 0.0001d);
    }
}
