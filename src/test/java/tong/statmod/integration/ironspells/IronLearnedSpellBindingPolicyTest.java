package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class IronLearnedSpellBindingPolicyTest {
    @Test
    void selectionRequiresServerLearnedEntryAndUsesServerLevel() {
        Map<String, Integer> learned = Map.of("irons_spellbooks:fireball", 4);

        assertEquals(4, LearnedSpellBindingPolicy.authorizedLevel(
                learned, "irons_spellbooks:fireball", 1, 5));
        assertEquals(0, LearnedSpellBindingPolicy.authorizedLevel(
                learned, "addon:not_learned", 1, 5));
        assertEquals(3, LearnedSpellBindingPolicy.authorizedLevel(
                Map.of("addon:overleveled", 99), "addon:overleveled", 1, 3));
    }

    @Test
    void reservedButtonIdsRoundTripWithinBound() {
        assertEquals(0, LearnedSpellBindingPolicy.optionFromButton(1000));
        assertEquals(511, LearnedSpellBindingPolicy.optionFromButton(1511));
        assertEquals(-1, LearnedSpellBindingPolicy.optionFromButton(999));
        assertEquals(-1, LearnedSpellBindingPolicy.optionFromButton(1512));
        assertEquals(1000, LearnedSpellBindingPolicy.buttonForOption(0));
        assertEquals(-1, LearnedSpellBindingPolicy.buttonForOption(512));
    }
}
