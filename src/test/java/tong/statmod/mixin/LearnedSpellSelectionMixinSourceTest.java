package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnedSpellSelectionMixinSourceTest {
    private static final Path SELECTION = Path.of("src", "main", "java", "tong", "statmod",
            "mixin", "SpellSelectionManagerMixin.java");

    @Test
    void learnedSpellsAreCastableFromTheHeldWeaponViaMainhandSlot() throws Exception {
        String selection = Files.readString(SELECTION);

        // Slot MAINHAND → serverSideInitiateCast (touche V) prend l'arme en main comme objet
        // support → le cast se complète. PAS de slot virtuel (sans objet, le cast n'aboutit pas).
        assertTrue(selection.contains("SpellSelectionManager.MAINHAND"),
                "les sorts appris doivent être castables depuis la main (touche V)");
        assertFalse(selection.contains("LearnedSpellCastPolicy.SLOT"),
                "ne doit plus utiliser le slot virtuel");

        // N'écrase pas un sort déjà proposé par un objet équipé (grimoire curio, staff).
        assertTrue(selection.contains("alreadyOffered"));
        // Sélection restaurée par identité de sort après mutation.
        assertTrue(selection.contains("previouslySelected"));
    }
}
