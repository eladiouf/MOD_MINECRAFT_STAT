package tong.statmod.integration.ironspells;

import tong.statmod.integration.ironspells.bridge.TensuraWrapperIds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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
    public static final int PAGE_SIZE = 6;

    private IronInscriptionKnownSpellIndex() {}

    /**
     * Liste dédoublonnée et triée des spells castables via la flow Iron's
     * (natifs + addons + wrappers Tensura).
     */
    public static List<String> learnedCastableSpellIds(Iterable<String> learnedSpells) {
        if (learnedSpells == null) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String spellId : learnedSpells) {
            if (spellId == null) continue;
            if (isCastableSpellId(spellId)) {
                unique.add(spellId);
            }
        }

        List<String> sorted = new ArrayList<>(unique);
        sorted.sort(Comparator.naturalOrder());
        return List.copyOf(sorted);
    }

    static boolean isCastableSpellId(String spellId) {
        if (TensuraWrapperIds.isWrapperId(spellId)) return true;
        if (spellId.startsWith("irons_spellbooks:")) return true;
        if (spellId.startsWith("gametechbcs_spellbooks:")) return true;
        if (spellId.startsWith("wind_spellbooks:")) return true;
        if (spellId.startsWith("legendarymage:")) return true;
        if (spellId.startsWith("darkdoppelganger:")) return true;
        return false;
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

    public static int optionIndexOf(List<String> all, String spellId) {
        if (all == null || spellId == null || spellId.isBlank()) {
            return -1;
        }
        return all.indexOf(spellId);
    }

    public static int normalizeSelectedOption(List<String> all, int selectedIndex, Predicate<String> predicate) {
        if (all == null || selectedIndex < 0 || selectedIndex >= all.size()) {
            return -1;
        }
        String selectedSpellId = all.get(selectedIndex);
        if (selectedSpellId == null) {
            return -1;
        }
        return predicate == null || predicate.test(selectedSpellId) ? selectedIndex : -1;
    }

    public static List<String> page(List<String> all, int page) {
        return page(all, page, null);
    }

    public static List<String> page(List<String> all, int page, Predicate<String> predicate) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        List<String> filtered = filtered(all, predicate);
        if (filtered.isEmpty()) {
            return List.of();
        }
        int safePage = Math.max(0, page);
        int maxPage = maxPage(filtered);
        if (safePage > maxPage) {
            return List.of();
        }
        int from = safePage * PAGE_SIZE;
        int to = Math.min(filtered.size(), from + PAGE_SIZE);
        return List.copyOf(filtered.subList(from, to));
    }

    public static int maxPage(Collection<String> all) {
        return maxPage(all, null);
    }

    public static int maxPage(Collection<String> all, Predicate<String> predicate) {
        if (all == null || all.isEmpty()) {
            return 0;
        }
        int size = filtered(all, predicate).size();
        if (size == 0) {
            return 0;
        }
        return Math.max(0, (size - 1) / PAGE_SIZE);
    }

    private static List<String> filtered(Collection<String> all, Predicate<String> predicate) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        if (predicate == null) {
            return List.copyOf(all);
        }
        List<String> filtered = new ArrayList<>();
        for (String value : all) {
            if (predicate.test(value)) {
                filtered.add(value);
            }
        }
        return List.copyOf(filtered);
    }
}
