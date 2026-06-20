package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsMageRulesTest {
    @Test
    void elfAwakensEarlierThanHumanAtSameStatSpread() {
        IntUnaryOperator levels = index -> switch (index) {
            case 7, 12 -> 12;
            case 14 -> 10;
            default -> 0;
        };

        assertTrue(ElementalsMageRules.canAwaken(ElementalsRaceAffinity.resolve("tensura:elf"), levels));
        assertFalse(ElementalsMageRules.canAwaken(ElementalsRaceAffinity.resolve("tensura:human"), levels));
    }

    @Test
    void humanUnlocksThirdBaseEarlierThanDwarf() {
        IntUnaryOperator levels = index -> switch (index) {
            case 7 -> 16;
            case 10 -> 20;
            case 14 -> 2;
            case 15 -> 16;
            default -> 0;
        };
        Set<Integer> perks = Set.of(Perk.ERUDITION_CORE.id, Perk.FIRE_CORE.id);

        assertTrue(ElementalsMageRules.canUnlockThirdBase(
                ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.FIRE,
                levels,
                perks,
                1));
        assertFalse(ElementalsMageRules.canUnlockThirdBase(
                ElementalsRaceAffinity.resolve("tensura:dwarf"),
                ElementalBranch.FIRE,
                levels,
                perks,
                1));
    }

    @Test
    void dwarfHasBestMetalGrimoireThreshold() {
        IntUnaryOperator levels = index -> switch (index) {
            case 7 -> 18;
            case 9 -> 20;
            case 10 -> 18;
            case 15 -> 4;
            default -> 0;
        };

        assertTrue(ElementalsMageRules.canUseRareGrimoire(
                ElementalsRaceAffinity.resolve("tensura:dwarf"),
                ElementalBranch.METAL,
                levels));
        assertFalse(ElementalsMageRules.canUseRareGrimoire(
                ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.METAL,
                levels));
    }

    @Test
    void unsupportedRaceCannotPassRareGrimoireGate() {
        IntUnaryOperator levels = index -> 30;

        assertFalse(ElementalsMageRules.canUseRareGrimoire(
                ElementalsRaceAffinity.resolve("tensura:slime"),
                ElementalBranch.METAL,
                levels));
    }

    @Test
    void unsupportedRaceCannotUnlockThirdBase() {
        IntUnaryOperator levels = index -> 30;
        Set<Integer> perks = Set.of(Perk.ERUDITION_CORE.id, Perk.FIRE_CORE.id);

        assertFalse(ElementalsMageRules.canUnlockThirdBase(
                ElementalsRaceAffinity.resolve("tensura:slime"),
                ElementalBranch.FIRE,
                levels,
                perks,
                1));
    }
}
