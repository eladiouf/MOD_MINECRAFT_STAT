package tong.statmod.integration.ironspells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IronInscriptionKnownSpellIndex {
    public static final int BUTTON_BASE = 1000;
    public static final int PAGE_SIZE = 5;

    private IronInscriptionKnownSpellIndex() {}

    public static List<String> learnedIronSpellIds(Iterable<String> learnedSpells) {
        if (learnedSpells == null) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String spellId : learnedSpells) {
            if (spellId != null && spellId.startsWith("irons_spellbooks:")) {
                unique.add(spellId);
            }
        }

        List<String> sorted = new ArrayList<>(unique);
        sorted.sort(Comparator.naturalOrder());
        return List.copyOf(sorted);
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
