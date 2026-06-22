package tong.statmod.integration.ironspells;

import tong.statmod.integration.ironspells.bridge.TensuraWrapperIds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Filtre les spells "inscriptables" depuis l'arbre Iron's Spellbooks.
 *
 * <p>Sont acceptés :
 * <ul>
 *   <li>Les spells natifs Iron's Spellbooks ({@code irons_spellbooks:*})</li>
 *   <li>Les wrappers Tensura enregistrés par STAT MOD
 *       ({@code statmod:tensura_*}) — voir
 *       {@link tong.statmod.integration.ironspells.bridge.TensuraSpellWrapperRegistry}.</li>
 * </ul>
 *
 * <p>Les IDs bruts {@code tensura:*} restent <b>filtrés</b> car ils ne sont pas dans le
 * registre Iron's ; ils existent dans {@code learnedSpells[]} pour la traçabilité mais ne
 * peuvent pas être inscrits directement.
 */
public final class IronInscriptionKnownSpellIndex {
    public static final int BUTTON_BASE = 1000;
    public static final int PAGE_SIZE = 5;

    private IronInscriptionKnownSpellIndex() {}

    /**
     * Liste dédoublonnée et triée des spells castables via la flow Iron's
     * (natifs + wrappers Tensura).
     */
    public static List<String> learnedCastableSpellIds(Iterable<String> learnedSpells) {
        if (learnedSpells == null) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String spellId : learnedSpells) {
            if (spellId == null) continue;
            if (spellId.startsWith("irons_spellbooks:") || TensuraWrapperIds.isWrapperId(spellId)) {
                unique.add(spellId);
            }
        }

        List<String> sorted = new ArrayList<>(unique);
        sorted.sort(Comparator.naturalOrder());
        return List.copyOf(sorted);
    }

    /**
     * @deprecated Conservé pour compatibilité avec les anciens appelants. Préférer
     *             {@link #learnedCastableSpellIds(Iterable)}.
     */
    @Deprecated
    public static List<String> learnedIronSpellIds(Iterable<String> learnedSpells) {
        return learnedCastableSpellIds(learnedSpells);
    }

    public static int buttonIdForOption(int optionIndex) {
        return BUTTON_BASE + Math.max(0, optionIndex);
    }

    public static int optionIndexFromButtonId(int buttonId) {
        return buttonId >= BUTTON_BASE ? buttonId - BUTTON_BASE : -1;
    }

    public static List<String> page(List<String> all, int page) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        int clampedPage = Math.max(0, Math.min(page, maxPage(all)));
        int from = clampedPage * PAGE_SIZE;
        int to = Math.min(all.size(), from + PAGE_SIZE);
        return List.copyOf(all.subList(from, to));
    }

    public static int maxPage(Collection<String> all) {
        if (all == null || all.isEmpty()) {
            return 0;
        }
        return Math.max(0, (all.size() - 1) / PAGE_SIZE);
    }
}
