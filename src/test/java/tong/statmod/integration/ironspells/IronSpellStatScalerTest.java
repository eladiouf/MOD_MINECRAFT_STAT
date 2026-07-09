package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronSpellStatScalerTest {
    private static final double EPSILON = 1.0e-9d;

    @Test
    void maxManaBonusBuildsALongCombatReserveFromManaPool() {
        assertEquals(400.0d, IronSpellStatScaler.maxManaBonus(0), EPSILON);
        assertEquals(775.0d, IronSpellStatScaler.maxManaBonus(25), EPSILON);
        assertEquals(1900.0d, IronSpellStatScaler.maxManaBonus(100), EPSILON);
    }

    @Test
    void manaRegenBonusKeepsPassiveRecoveryVisibleWithoutTrivializingDowntime() {
        assertEquals(1.0000d, IronSpellStatScaler.manaRegenBonus(0), EPSILON);
        assertEquals(3.8000d, IronSpellStatScaler.manaRegenBonus(20), EPSILON);
        assertEquals(15.0000d, IronSpellStatScaler.manaRegenBonus(100), EPSILON);
    }

    @Test
    void manaRegenBonusRefillsEmptyManaPoolsInsideTheTargetCombatWindow() {
        assertEquals(400.0d, secondsToFullFromEmpty(0), 1.0e-6d);
        assertEquals(184.2105263158d, secondsToFullFromEmpty(20), 1.0e-6d);
        assertEquals(126.6666666667d, secondsToFullFromEmpty(100), 1.0e-6d);
    }

    @Test
    void spellPowerAndResistBonuses_followMagicCoreLevels() {
        assertEquals(0.0d, IronSpellStatScaler.spellPowerBonus(0), EPSILON);
        assertEquals(0.15d, IronSpellStatScaler.spellPowerBonus(100), EPSILON);
        assertEquals(0.15d, IronSpellStatScaler.spellResistBonus(50), EPSILON);
    }

    @Test
    void magicCorePerksAddAdvertisedBaselinePower() {
        assertEquals(0.20d, IronSpellStatScaler.spellPowerBonus(100, true), EPSILON);
        assertEquals(0.20d, IronSpellStatScaler.spellResistBonus(50, true), EPSILON);
        assertEquals(0.35d, IronSpellStatScaler.castTimeReductionBonus(100, true), EPSILON);
        assertEquals(0.25d, IronSpellStatScaler.cooldownReductionBonus(50, true), EPSILON);
        assertEquals(1950.0d, IronSpellStatScaler.maxManaBonus(100, true), EPSILON);
    }

    @Test
    void castBonuses_splitBetweenCastingSpeedAndErudition() {
        assertEquals(0.0d, IronSpellStatScaler.castTimeReductionBonus(0), EPSILON);
        assertEquals(0.30d, IronSpellStatScaler.castTimeReductionBonus(100), EPSILON);
        assertEquals(0.20d, IronSpellStatScaler.cooldownReductionBonus(50), EPSILON);
    }

    @Test
    void elementalSpellPowerBonusUsesMatchingAffinityOnly() {
        assertEquals(0.15d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.FIRE, 100, 0, 0, 0), EPSILON);
        assertEquals(0.15d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.WATER, 0, 100, 0, 0), EPSILON);
        assertEquals(0.15d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.EARTH, 0, 0, 100, 0), EPSILON);
        assertEquals(0.15d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.AIR, 0, 0, 0, 100), EPSILON);
    }

    @Test
    void elementalCorePerksOnlyBoostTheirMatchingBranch() {
        assertEquals(0.20d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.FIRE, 100, 0, 0, 0, true, false, false, false), EPSILON);
        assertEquals(0.20d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.WATER, 0, 100, 0, 0, false, true, false, false), EPSILON);
        assertEquals(0.20d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.EARTH, 0, 0, 100, 0, false, false, true, false), EPSILON);
        assertEquals(0.20d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.AIR, 0, 0, 0, 100, false, false, false, true), EPSILON);
        assertEquals(0.15d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.FIRE, 100, 0, 0, 0, false, true, true, true), EPSILON);
    }

    @Test
    void nonElementalBranchesDoNotReceiveElementalAffinityBonus() {
        assertEquals(0.0d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.HOLY, 100, 100, 100, 100), EPSILON);
        assertEquals(0.0d, IronSpellStatScaler.elementalSpellPowerBonus(
                MagicBranch.EVOCATION, 100, 100, 100, 100), EPSILON);
        assertEquals(0.0d, IronSpellStatScaler.elementalSpellPowerBonus(
                null, 100, 100, 100, 100), EPSILON);
    }

    private static double secondsToFullFromEmpty(int manaPoolLevel) {
        double maxMana = IronSpellStatScaler.maxManaBonus(manaPoolLevel);
        double regen = IronSpellStatScaler.manaRegenBonus(manaPoolLevel);
        return maxMana / regen;
    }
}
