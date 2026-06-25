package tong.statmod.client.inscription;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public record KnownSpellUiState(List<String> visibleSpellIds,
                                int page,
                                int selectedOption,
                                boolean spellBookSlotted,
                                Set<String> boundSpellIds) {
    public static KnownSpellUiState capture(List<String> visibleSpellIds,
                                            int page,
                                            int selectedOption,
                                            boolean spellBookSlotted) {
        return capture(visibleSpellIds, page, selectedOption, spellBookSlotted, Set.of());
    }

    public static KnownSpellUiState capture(List<String> visibleSpellIds,
                                            int page,
                                            int selectedOption,
                                            boolean spellBookSlotted,
                                            Set<String> boundSpellIds) {
        return new KnownSpellUiState(
                List.copyOf(visibleSpellIds),
                page,
                selectedOption,
                spellBookSlotted,
                boundSpellIds == null ? Set.of() : Collections.unmodifiableSet(Set.copyOf(boundSpellIds)));
    }
}
