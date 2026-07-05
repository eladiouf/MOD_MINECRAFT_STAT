package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronSpellAdvancedPerkScalingTest {
    private static final double EPSILON = 1.0e-9d;

    @Test
    void advancedAttributeBonusAddsEachUnlockedTier() {
        assertEquals(0.0d, IronSpellAdvancedPerkScaling.advancedPercentAttributeBonus(
                false, false, false, false, false), EPSILON);
        assertEquals(0.29d, IronSpellAdvancedPerkScaling.advancedPercentAttributeBonus(
                true, true, true, true, true), EPSILON);
    }

    @Test
    void advancedManaBonusUsesReserveSizedFlatBonuses() {
        assertEquals(0.0d, IronSpellAdvancedPerkScaling.advancedManaBonus(
                false, false, false, false, false), EPSILON);
        assertEquals(60.0d, IronSpellAdvancedPerkScaling.advancedManaBonus(
                true, true, true, true, true), EPSILON);
    }

    @Test
    void arcaneDamageMultiplierCombinesBurstSynergySituationAndCascade() {
        assertEquals(1.0d, IronSpellAdvancedPerkScaling.arcaneDamageMultiplier(
                false, false, false, 0, false, false), EPSILON);
        assertEquals(1.36d, IronSpellAdvancedPerkScaling.arcaneDamageMultiplier(
                true, true, true, 3, true, false), EPSILON);
        assertEquals(1.56d, IronSpellAdvancedPerkScaling.arcaneDamageMultiplier(
                true, true, true, 3, true, true), EPSILON);
    }

    @Test
    void elementalDamageMultiplierOnlyAppliesToMatchingBranch() {
        assertEquals(1.47d, IronSpellAdvancedPerkScaling.elementalDamageMultiplier(
                MagicBranch.FIRE, MagicBranch.FIRE, true, true, true, 3, true, true), EPSILON);
        assertEquals(1.0d, IronSpellAdvancedPerkScaling.elementalDamageMultiplier(
                MagicBranch.WATER, MagicBranch.FIRE, true, true, true, 3, true, true), EPSILON);
    }

    @Test
    void manaRefundScalesWithUnlockedReservePerks() {
        assertEquals(0.0d, IronSpellAdvancedPerkScaling.manaRefund(100, false, false, false), EPSILON);
        assertEquals(10.0d, IronSpellAdvancedPerkScaling.manaRefund(100, true, false, false), EPSILON);
        assertEquals(25.0d, IronSpellAdvancedPerkScaling.manaRefund(100, true, true, true), EPSILON);
    }
}
