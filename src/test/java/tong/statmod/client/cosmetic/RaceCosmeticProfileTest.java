package tong.statmod.client.cosmetic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RaceCosmeticProfileTest {
    @Test
    void evolved_elf_races_keep_elf_cosmetics() {
        assertEquals(RaceCosmeticProfile.ELF, RaceCosmeticProfile.resolve("tensura:elf"));
        assertEquals(RaceCosmeticProfile.ELF, RaceCosmeticProfile.resolve("tensura:elf_saint"));
        assertEquals(RaceCosmeticProfile.ELF, RaceCosmeticProfile.resolve("tensura:divine_elf"));
    }

    @Test
    void evolved_dwarf_races_keep_dwarf_cosmetics() {
        assertEquals(RaceCosmeticProfile.DWARF, RaceCosmeticProfile.resolve("tensura:dwarf"));
        assertEquals(RaceCosmeticProfile.DWARF, RaceCosmeticProfile.resolve("tensura:dwarf_saint"));
        assertEquals(RaceCosmeticProfile.DWARF, RaceCosmeticProfile.resolve("tensura:divine_dwarf"));
    }

    @Test
    void evolved_beast_races_keep_beastfolk_cosmetics() {
        assertEquals(RaceCosmeticProfile.BEASTFOLK, RaceCosmeticProfile.resolve("tensura:beastfolk"));
        assertEquals(RaceCosmeticProfile.BEASTFOLK, RaceCosmeticProfile.resolve("tensura:beast_lord"));
        assertEquals(RaceCosmeticProfile.BEASTFOLK, RaceCosmeticProfile.resolve("tensura:spirit_beast"));
        assertEquals(RaceCosmeticProfile.BEASTFOLK, RaceCosmeticProfile.resolve("tensura:divine_beast"));
    }

    @Test
    void human_unknown_and_blank_races_render_no_extra_cosmetics() {
        assertEquals(RaceCosmeticProfile.NONE, RaceCosmeticProfile.resolve("tensura:human"));
        assertEquals(RaceCosmeticProfile.NONE, RaceCosmeticProfile.resolve("tensura:phoenix"));
        assertEquals(RaceCosmeticProfile.NONE, RaceCosmeticProfile.resolve(""));
        assertEquals(RaceCosmeticProfile.NONE, RaceCosmeticProfile.resolve(null));
    }
}
