package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsMageRulesTest {
    private static IntUnaryOperator levels(int... values) {
        return index -> index >= 0 && index < values.length ? values[index] : 0;
    }

    @Test
    void mageAwakeningUsesTheApprovedThresholdBands() {
        MageRaceProfile elf = ElementalsRaceAffinity.resolve("tensura:elf");
        MageRaceProfile beastfolk = ElementalsRaceAffinity.resolve("tensura:beastfolk");
        assertTrue(ElementalsMageRules.canAwaken(elf, levels(
                0, 0, 0, 0, 0, 0,
                0, 12, 0, 0, 0, 0, 12, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        assertFalse(ElementalsMageRules.canAwaken(beastfolk, levels(
                0, 0, 0, 0, 0, 0,
                0, 12, 0, 0, 0, 0, 12, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    void startingElementBecomesMasteredOnlyWithStatsAndPerk() {
        IntUnaryOperator levels = levels(
                0, 0, 0, 0, 0, 0,
                0, 10, 0, 0, 18, 0, 0, 14, 0, 6, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(ElementState.AWAKENED,
                ElementalsMageRules.stateForBaseBranch(ElementalBranch.FIRE, levels, Set.of()));
        assertEquals(ElementState.MASTERED,
                ElementalsMageRules.stateForBaseBranch(ElementalBranch.FIRE, levels, Set.of(Perk.FIRE_MASTERY.id)));
    }

    @Test
    void thirdAndFourthBaseUnlocksRespectHumanAndBeastfolkModifiers() {
        IntUnaryOperator humanLevels = levels(
                0, 0, 0, 0, 0, 0,
                0, 18, 0, 22, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0);
        MageRaceProfile human = ElementalsRaceAffinity.resolve("tensura:human");
        assertTrue(ElementalsMageRules.canUnlockThirdBase(
                human,
                ElementalBranch.EARTH,
                humanLevels,
                Set.of(Perk.ERUDITION_CORE.id, Perk.EARTH_CORE.id),
                1));

        MageRaceProfile beastfolk = ElementalsRaceAffinity.resolve("tensura:beastfolk");
        assertFalse(ElementalsMageRules.canUnlockThirdBase(
                beastfolk,
                ElementalBranch.EARTH,
                humanLevels,
                Set.of(Perk.ERUDITION_CORE.id, Perk.EARTH_CORE.id),
                1));
    }

    @Test
    void rareGrimoireThresholdsUseTheApprovedPrimaryStats() {
        IntUnaryOperator lightning = levels(
                0, 0, 0, 0, 0, 0,
                0, 20, 0, 0, 0, 0, 0, 20, 0, 18, 0, 0, 0, 0, 0, 0, 0);
        IntUnaryOperator blood = levels(
                0, 0, 0, 0, 0, 0,
                0, 20, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 20);
        assertTrue(ElementalsMageRules.canUseRareGrimoire(ElementalBranch.LIGHTNING, lightning));
        assertTrue(ElementalsMageRules.canUseRareGrimoire(ElementalBranch.BLOOD, blood));
    }
}
