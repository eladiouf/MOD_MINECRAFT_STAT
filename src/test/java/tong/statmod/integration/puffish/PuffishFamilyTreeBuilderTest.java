package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishFamilyTreeBuilderTest {
    @Test
    void mapsPerksIntoFamilyCategories() {
        assertEquals("statmod:frontline_physical_combat", PuffishPerkIds.categoryId(Perk.BRUTE_CORE));
        assertEquals("statmod:magical_core", PuffishPerkIds.categoryId(Perk.ARCANE_CORE));
        assertEquals("arcane_power__arcane_core", PuffishPerkIds.skillId(Perk.ARCANE_CORE));
    }

    @Test
    void buildsSixFamilyTabsWithGeneratedMagicalSkills() {
        assertTrue(PuffishFamilyTreeBuilder.configJson().contains("\"magical_core\""));
        assertTrue(PuffishFamilyTreeBuilder.configJson().contains("\"elemental_specialization\""));

        PuffishFamilyTreeBuilder.GeneratedCategoryFiles files =
                PuffishFamilyTreeBuilder.categoryFiles(tong.statmod.stats.StatFamily.MAGICAL_CORE);
        assertTrue(files.skillsJson().contains("\"arcane_power__arcane_core\""));
        assertTrue(files.definitionsJson().contains("Spell Pressure"));
    }
}
