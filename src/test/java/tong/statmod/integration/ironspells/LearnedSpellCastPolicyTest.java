package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LearnedSpellCastPolicyTest {
    @Test
    void virtualSlotHasStableNamespacedIdentity() {
        assertEquals("statmod_learned", LearnedSpellCastPolicy.SLOT);
        assertTrue(LearnedSpellCastPolicy.isVirtualSlot("statmod_learned"));
        assertFalse(LearnedSpellCastPolicy.isVirtualSlot("mainhand"));
        assertFalse(LearnedSpellCastPolicy.isVirtualSlot(null));
    }

    @Test
    void virtualSelectionRequiresServerLearnedSpell() {
        List<String> learned = List.of("irons_spellbooks:fireball");

        assertTrue(LearnedSpellCastPolicy.canUseVirtualSelection(
                "statmod_learned", "irons_spellbooks:fireball", learned));
        assertFalse(LearnedSpellCastPolicy.canUseVirtualSelection(
                "statmod_learned", "irons_spellbooks:teleport", learned));
        assertFalse(LearnedSpellCastPolicy.canUseVirtualSelection(
                "mainhand", "irons_spellbooks:fireball", learned));
        assertFalse(LearnedSpellCastPolicy.canUseVirtualSelection(
                "statmod_learned", "", learned));
    }
}
