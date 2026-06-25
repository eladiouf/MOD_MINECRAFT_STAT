package tong.statmod.client.inscription;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class KnownSpellUiStateTest {
    @Test
    void states_with_same_visible_inputs_are_equal() {
        KnownSpellUiState left = KnownSpellUiState.capture(List.of("a", "b"), 1, 3, true);
        KnownSpellUiState right = KnownSpellUiState.capture(List.of("a", "b"), 1, 3, true);

        assertEquals(left, right);
    }

    @Test
    void state_changes_when_page_or_slot_state_changes() {
        KnownSpellUiState base = KnownSpellUiState.capture(List.of("a", "b"), 0, 1, false);

        assertNotEquals(base, KnownSpellUiState.capture(List.of("a", "b"), 1, 1, false));
        assertNotEquals(base, KnownSpellUiState.capture(List.of("a", "b"), 0, 1, true));
        assertNotEquals(base, KnownSpellUiState.capture(List.of("a", "c"), 0, 1, false));
    }

    @Test
    void state_changes_when_bound_spell_set_changes() {
        KnownSpellUiState empty = KnownSpellUiState.capture(
                List.of("a", "b"), 0, 1, true, Set.of());
        KnownSpellUiState withBound = KnownSpellUiState.capture(
                List.of("a", "b"), 0, 1, true, Set.of("a"));

        assertNotEquals(empty, withBound,
                "inscribing a new spell should invalidate the cached UI state");
    }

    @Test
    void state_is_equal_when_bound_spell_set_matches() {
        KnownSpellUiState left = KnownSpellUiState.capture(
                List.of("a", "b"), 0, 1, true, Set.of("a", "b"));
        KnownSpellUiState right = KnownSpellUiState.capture(
                List.of("a", "b"), 0, 1, true, Set.of("b", "a"));

        assertEquals(left, right,
                "bound spell set equality must be order-independent");
    }

    @Test
    void default_capture_treats_bound_set_as_empty() {
        KnownSpellUiState legacy = KnownSpellUiState.capture(List.of("a"), 0, 0, true);

        assertEquals(Set.of(), legacy.boundSpellIds());
    }
}
