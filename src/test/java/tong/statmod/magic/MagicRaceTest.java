package tong.statmod.magic;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MagicRaceTest {
    @Test
    void human_has_two_weak_universal_affinities() {
        assertEquals(Set.of(MagicBranch.FIRE, MagicBranch.WATER, MagicBranch.AIR, MagicBranch.EARTH),
                MagicRace.HUMAN.naturalAffinities());
        assertTrue(MagicRace.HUMAN.flexible);
    }

    @Test
    void elf_air_water() {
        assertEquals(Set.of(MagicBranch.AIR, MagicBranch.WATER), MagicRace.ELF.naturalAffinities());
        assertFalse(MagicRace.ELF.flexible);
    }

    @Test
    void dwarf_earth_fire() {
        assertEquals(Set.of(MagicBranch.EARTH, MagicBranch.FIRE), MagicRace.DWARF.naturalAffinities());
    }

    @Test
    void beast_water_air_weaker_purity() {
        assertEquals(Set.of(MagicBranch.WATER, MagicBranch.AIR), MagicRace.BEAST.naturalAffinities());
        assertTrue(MagicRace.BEAST.purityPenalty);
    }

    @Test
    void start_branch_selection_obeys_racial_affinities() {
        assertTrue(MagicRace.HUMAN.canChooseStartBranch(MagicBranch.EARTH));
        assertTrue(MagicRace.DWARF.canChooseStartBranch(MagicBranch.FIRE));
        assertFalse(MagicRace.DWARF.canChooseStartBranch(MagicBranch.WATER));
        assertFalse(MagicRace.ELF.canChooseStartBranch(MagicBranch.HOLY));
        assertFalse(MagicRace.BEAST.canChooseStartBranch(MagicBranch.COMMON));
    }

    @Test
    void starting_race_catalog_is_canonical_and_stable() {
        assertEquals("tensura:human", MagicRace.HUMAN.tensuraStartingRaceId());
        assertEquals("tensura:elf", MagicRace.ELF.tensuraStartingRaceId());
        assertEquals("tensura:dwarf", MagicRace.DWARF.tensuraStartingRaceId());
        assertEquals("tensura:beastfolk", MagicRace.BEAST.tensuraStartingRaceId());

        assertEquals(Set.of(
                ResourceLocation.fromNamespaceAndPath("tensura", "human"),
                ResourceLocation.fromNamespaceAndPath("tensura", "elf"),
                ResourceLocation.fromNamespaceAndPath("tensura", "dwarf"),
                ResourceLocation.fromNamespaceAndPath("tensura", "beastfolk")
        ), MagicRace.allowedStartingRaceIds());
    }

    @Test
    void race_catalog_exposes_stable_display_identity() {
        assertEquals("Human", MagicRace.HUMAN.displayName());
        assertEquals("Elf", MagicRace.ELF.displayName());
        assertEquals("Dwarf", MagicRace.DWARF.displayName());
        assertEquals("Beastfolk", MagicRace.BEAST.displayName());

        assertEquals(List.of(MagicBranch.FIRE, MagicBranch.WATER, MagicBranch.AIR, MagicBranch.EARTH),
                MagicRace.HUMAN.orderedAffinities());
        assertEquals(List.of(MagicBranch.AIR, MagicBranch.WATER), MagicRace.ELF.orderedAffinities());
        assertEquals(List.of(MagicBranch.EARTH, MagicBranch.FIRE), MagicRace.DWARF.orderedAffinities());
        assertEquals(List.of(MagicBranch.WATER, MagicBranch.AIR), MagicRace.BEAST.orderedAffinities());
    }

    @Test
    void default_start_branch_is_explicit_per_race() {
        assertEquals(MagicBranch.FIRE, MagicRace.HUMAN.defaultStartBranch());
        assertEquals(MagicBranch.AIR, MagicRace.ELF.defaultStartBranch());
        assertEquals(MagicBranch.EARTH, MagicRace.DWARF.defaultStartBranch());
        assertEquals(MagicBranch.WATER, MagicRace.BEAST.defaultStartBranch());
    }
}
