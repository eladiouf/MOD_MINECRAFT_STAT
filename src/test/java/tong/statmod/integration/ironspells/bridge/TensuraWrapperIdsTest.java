package tong.statmod.integration.ironspells.bridge;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.tensura.TensuraSpellTaxonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraWrapperIdsTest {
    @Test
    void wrapperIdForReplacesColonWithUnderscoreAndKeepsNamespace() {
        assertEquals("statmod:tensura_fire_bolt", TensuraWrapperIds.wrapperIdFor("tensura:fire_bolt"));
        assertEquals("statmod:tensura_hellfire", TensuraWrapperIds.wrapperIdFor("tensura:hellfire"));
        assertEquals("statmod:tensura_anti_magic_area", TensuraWrapperIds.wrapperIdFor("tensura:anti_magic_area"));
    }

    @Test
    void pathForStripsNamespace() {
        assertEquals("tensura_fire_bolt", TensuraWrapperIds.pathFor("tensura:fire_bolt"));
        assertEquals("tensura_fire_bolt", TensuraWrapperIds.pathFor("fire_bolt")); // namespace-less input
    }

    @Test
    void isWrapperIdRecognizesPrefixOnly() {
        assertTrue(TensuraWrapperIds.isWrapperId("statmod:tensura_fire_bolt"));
        assertFalse(TensuraWrapperIds.isWrapperId("irons_spellbooks:fireball"));
        assertFalse(TensuraWrapperIds.isWrapperId("tensura:fire_bolt"));
        assertFalse(TensuraWrapperIds.isWrapperId(null));
        assertFalse(TensuraWrapperIds.isWrapperId("statmod:something_else"));
    }

    @Test
    void everyKnownTaxonomySkillProducesUniqueSanitizedWrapperId() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String skillId : TensuraSpellTaxonomy.allSkillIds()) {
            String wrapper = TensuraWrapperIds.wrapperIdFor(skillId);
            assertNotEquals(skillId, wrapper, "wrapper id must differ from raw tensura id");
            assertTrue(wrapper.startsWith("statmod:tensura_"), "wrapper id format: " + wrapper);
            assertTrue(seen.add(wrapper), "duplicate wrapper id detected: " + wrapper);
        }
        // sanity check: the taxonomy provides the full 56-spell scope
        assertEquals(56, seen.size());
    }

    @Test
    void displayKeyAndIconKeyAreStable() {
        assertEquals("statmod.spell.tensura.fire_bolt.name",
                TensuraWrapperIds.displayNameTranslationKey("tensura:fire_bolt"));
        assertEquals("statmod:textures/spell/tensura/default",
                TensuraWrapperIds.iconTextureKey("tensura:fire_bolt"));
    }
}
