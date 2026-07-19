package tong.statmod.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScrollLearningServiceTest {
    @Test
    void newAndUpgradeConsumeButDuplicatesDoNot() {
        LearnedSpellState state = new LearnedSpellState();

        ScrollLearningService.Outcome first = ScrollLearningService.apply(
                state, "irons_spellbooks:fireball", 2, 1, 5);
        assertEquals(ScrollLearningService.Status.LEARNED, first.status());
        assertTrue(first.consume());

        ScrollLearningService.Outcome lower = ScrollLearningService.apply(
                state, "irons_spellbooks:fireball", 1, 1, 5);
        assertEquals(ScrollLearningService.Status.ALREADY_KNOWN, lower.status());
        assertFalse(lower.consume());

        ScrollLearningService.Outcome higher = ScrollLearningService.apply(
                state, "irons_spellbooks:fireball", 99, 1, 5);
        assertEquals(ScrollLearningService.Status.UPGRADED, higher.status());
        assertEquals(5, higher.newLevel());
        assertTrue(higher.consume());
    }

    @Test
    void invalidBoundsAndFullLibraryNeverConsume() {
        assertEquals(ScrollLearningService.Status.INVALID,
                ScrollLearningService.apply(null, "addon:spell", 1, 1, 5).status());
        assertEquals(ScrollLearningService.Status.INVALID,
                ScrollLearningService.apply(new LearnedSpellState(), "addon:spell", 1, 0, 5).status());

        LearnedSpellState full = new LearnedSpellState();
        for (int index = 0; index < LearnedSpellState.MAX_ENTRIES; index++) {
            full.learn("addon:spell_" + index, 1);
        }
        ScrollLearningService.Outcome outcome = ScrollLearningService.apply(
                full, "addon:overflow", 1, 1, 5);
        assertEquals(ScrollLearningService.Status.FULL, outcome.status());
        assertFalse(outcome.consume());
    }
}
