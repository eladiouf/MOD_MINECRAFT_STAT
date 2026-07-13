package tong.statmod.integration.ironspells;

import java.util.Collection;

/**
 * Politique du slot de cast virtuel des sorts appris via STAT Mod (2026-07-13).
 *
 * <p>Les sorts appris dans l'arbre magique doivent être castables avec n'importe quel objet
 * en main (ou rien). Ils reçoivent un slot de sélection virtuel stable {@link #SLOT} qui
 * prime sur les options fournies par l'équipement — le cast passe alors par la source
 * {@code SPELLBOOK} d'Iron's Spellbooks (voir {@code IronLearnedSpellCastSourceMixin}).
 * Classe pure : uniquement collections/chaînes Java, testable sans jeu.
 */
public final class LearnedSpellCastPolicy {
    public static final String SLOT = "statmod_learned";

    private LearnedSpellCastPolicy() {
    }

    public static boolean isVirtualSlot(String slot) {
        return SLOT.equals(slot);
    }

    public static boolean canUseVirtualSelection(String slot, String spellId,
                                                 Collection<String> learnedSpellIds) {
        return isVirtualSlot(slot)
                && spellId != null
                && !spellId.isBlank()
                && learnedSpellIds != null
                && learnedSpellIds.contains(spellId);
    }
}
