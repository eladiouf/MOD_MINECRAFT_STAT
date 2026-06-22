package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishFamilyTreeBuilderTest {
    @Test
    void mapsPerksIntoUnifiedCategory() {
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.BRUTE_CORE));
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.ARCANE_CORE));
        assertEquals("arcane_power__arcane_core", PuffishPerkIds.skillId(Perk.ARCANE_CORE));
    }

    @Test
    void buildsOneUnifiedPerkTabAlongsideMagicTabs() {
        assertTrue(PuffishFamilyTreeBuilder.configJson().contains("\"statmod_perks\""));
        assertTrue(PuffishFamilyTreeBuilder.configJson().contains("\"statmod_magic\""));

        PuffishFamilyTreeBuilder.GeneratedCategoryFiles files =
                PuffishFamilyTreeBuilder.unifiedCategoryFiles();
        assertTrue(files.skillsJson().contains("\"arcane_power__arcane_core\""));
        assertTrue(files.skillsJson().contains("\"forging__forge_core\""));
        assertTrue(files.definitionsJson().contains("Spell Pressure"));
        assertTrue(files.categoryJson().contains("Perk Tree"));
    }
}
