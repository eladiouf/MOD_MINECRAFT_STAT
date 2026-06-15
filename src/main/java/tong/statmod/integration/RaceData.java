package tong.statmod.integration;

import java.util.Collections;
import java.util.List;

public record RaceData(
        List<RaceModifier> modifiers,
        List<Integer> exclusivePerks
) {
    public static final RaceData EMPTY = new RaceData(Collections.emptyList(), Collections.emptyList());
}
