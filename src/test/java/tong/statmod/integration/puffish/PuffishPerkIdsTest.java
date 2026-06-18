package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PuffishPerkIdsTest {
    @Test
    void mapsPerkToDeterministicCategoryAndSkillIds() {
        assertEquals("statmod:frontline_physical_combat", PuffishPerkIds.categoryId(Perk.BLADE_CORE));
        assertEquals("blade_technique__blade_core", PuffishPerkIds.skillId(Perk.BLADE_CORE));
        assertEquals("statmod:frontline_physical_combat", PuffishPerkIds.categoryId(Perk.ENDUR_TRANSCENDENCE));
    }

    @Test
    void resolvesPerkBackFromCategoryAndSkill() {
        assertSame(Perk.BRUTE_CORE, PuffishPerkIds.resolve("statmod:frontline_physical_combat", "brute_force__brute_core"));
        assertSame(Perk.WILL_TRANSCENDENCE, PuffishPerkIds.resolve("statmod:mental_pressure_resilience", "willpower__will_transcendence"));
    }
}
