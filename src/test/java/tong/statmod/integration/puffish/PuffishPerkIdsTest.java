package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PuffishPerkIdsTest {
    @Test
    void mapsPerkToDeterministicCategoryAndSkillIds() {
        assertEquals("statmod:blade_technique", PuffishPerkIds.categoryId(Perk.BLADE_CORE));
        assertEquals("blade_core", PuffishPerkIds.skillId(Perk.BLADE_CORE));
        assertEquals("statmod:physical_endurance", PuffishPerkIds.categoryId(Perk.ENDUR_TRANSCENDENCE));
    }

    @Test
    void resolvesPerkBackFromCategoryAndSkill() {
        assertSame(Perk.BRUTE_CORE, PuffishPerkIds.resolve("statmod:brute_force", "brute_core"));
        assertSame(Perk.WILL_TRANSCENDENCE, PuffishPerkIds.resolve("statmod:willpower", "will_transcendence"));
    }
}
