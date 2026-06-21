package tong.statmod.magic;

import org.junit.jupiter.api.Test;
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
}
