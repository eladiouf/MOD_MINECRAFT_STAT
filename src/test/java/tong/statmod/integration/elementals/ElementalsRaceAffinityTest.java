package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsRaceAffinityTest {
    @Test
    void supportsOnlyTheFourApprovedMageRaces() {
        assertTrue(ElementalsRaceAffinity.resolve("tensura:elf").supported());
        assertTrue(ElementalsRaceAffinity.resolve("tensura:human").supported());
        assertTrue(ElementalsRaceAffinity.resolve("tensura:dwarf").supported());
        assertTrue(ElementalsRaceAffinity.resolve("tensura:beastfolk").supported());
        assertFalse(ElementalsRaceAffinity.resolve("tensura:slime").supported());
    }

    @Test
    void fixedRacesKeepTheirApprovedStarterPairs() {
        assertEquals(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER),
                ElementalsRaceAffinity.starterBranches(
                        ElementalsRaceAffinity.resolve("tensura:elf"),
                        UUID.fromString("00000000-0000-0000-0000-000000000001")));
        assertEquals(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH),
                ElementalsRaceAffinity.starterBranches(
                        ElementalsRaceAffinity.resolve("tensura:dwarf"),
                        UUID.fromString("00000000-0000-0000-0000-000000000002")));
        assertEquals(EnumSet.of(ElementalBranch.WATER, ElementalBranch.AIR),
                ElementalsRaceAffinity.starterBranches(
                        ElementalsRaceAffinity.resolve("tensura:beastfolk"),
                        UUID.fromString("00000000-0000-0000-0000-000000000003")));
    }

    @Test
    void humansGetDeterministicBaseOnlyStarterPairs() {
        MageRaceProfile profile = ElementalsRaceAffinity.resolve("tensura:human");
        EnumSet<ElementalBranch> starters = ElementalsRaceAffinity.starterBranches(
                profile,
                UUID.fromString("11111111-2222-3333-4444-555555555555"));
        assertEquals(2, starters.size());
        assertFalse(starters.contains(ElementalBranch.LIGHTNING));
        assertFalse(starters.contains(ElementalBranch.BLOOD));
    }
}
