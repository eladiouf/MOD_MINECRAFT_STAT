package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TensuraToMagicRaceMapperTest {
    @Test
    void direct_families_map_to_matching_magic_race() {
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:human"));
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:elf"));
        assertEquals(MagicRace.DWARF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:dwarf"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:beastfolk"));
    }

    @Test
    void evolution_tiers_map_to_base_family() {
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:divine_elf"));
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:elf_saint"));
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:enlightened_elf"));
        assertEquals(MagicRace.DWARF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:dwarf_saint"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:divine_beast"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:beast_lord"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:spirit_beast"));
        assertEquals(MagicRace.DWARF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:ancient_giant"));
    }

    @Test
    void thematic_families_map_to_closest_magic_race() {
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:goblin"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:hobgoblin_saint"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:ogre"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:orc"));
        assertEquals(MagicRace.BEAST, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:lizardman"));
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:slime"));
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:merfolk"));
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:harpy"));
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:daemon"));
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:arch_daemon"));
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:vampire"));
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:wight"));
        assertEquals(MagicRace.DWARF, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:giant"));
    }

    @Test
    void null_and_unknown_default_to_human() {
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId(null));
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId("tensura:phoenix"));
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId(""));
    }

    @Test
    void no_namespace_still_resolves_family() {
        assertEquals(MagicRace.ELF, TensuraToMagicRaceMapper.fromTensuraRaceId("elf"));
        assertEquals(MagicRace.HUMAN, TensuraToMagicRaceMapper.fromTensuraRaceId("HUMAN"));
    }

    @Test
    void default_start_branch_picks_first_affinity_for_non_flexible() {
        MagicBranch elfStart = TensuraToMagicRaceMapper.defaultStartBranch(MagicRace.ELF);
        assertEquals(MagicBranch.AIR, elfStart);

        MagicBranch dwarfStart = TensuraToMagicRaceMapper.defaultStartBranch(MagicRace.DWARF);
        assertEquals(MagicBranch.EARTH, dwarfStart);

        MagicBranch beastStart = TensuraToMagicRaceMapper.defaultStartBranch(MagicRace.BEAST);
        assertEquals(MagicBranch.WATER, beastStart);
    }

    @Test
    void default_start_branch_for_flexible_is_fire() {
        assertEquals(MagicBranch.FIRE, TensuraToMagicRaceMapper.defaultStartBranch(MagicRace.HUMAN));
    }

    @Test
    void default_start_branch_null_safe() {
        assertEquals(MagicBranch.FIRE, TensuraToMagicRaceMapper.defaultStartBranch(null));
    }

    @Test
    void ui_display_names_are_humanized_from_tensura_ids() {
        assertEquals("Human", TensuraToMagicRaceMapper.displayRaceName("tensura:human"));
        assertEquals("Divine Elf", TensuraToMagicRaceMapper.displayRaceName("tensura:divine_elf"));
        assertEquals("Beastfolk", TensuraToMagicRaceMapper.displayRaceName("tensura:beastfolk"));
        assertEquals("Arch Daemon", TensuraToMagicRaceMapper.displayRaceName("tensura:arch_daemon"));
        assertEquals("Unknown", TensuraToMagicRaceMapper.displayRaceName(null));
    }
}
