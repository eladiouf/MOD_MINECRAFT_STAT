package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PuffishPerkIdsTest {
    @Test
    void mapsEveryPerkToTheUnifiedCategory() {
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.BLADE_CORE));
        assertEquals("blade_technique__blade_core", PuffishPerkIds.skillId(Perk.BLADE_CORE));
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.ENDUR_TRANSCENDENCE));
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.WILL_TRANSCENDENCE));
    }

    @Test
    void resolvesPerkBackFromUnifiedCategoryAndSkill() {
        assertSame(Perk.BRUTE_CORE,
                PuffishPerkIds.resolve("statmod:statmod_perks", "brute_force__brute_core"));
        assertSame(Perk.WILL_TRANSCENDENCE,
                PuffishPerkIds.resolve("statmod:statmod_perks", "willpower__will_transcendence"));
    }
}
