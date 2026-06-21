package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit smoke for the opener service. The full menu-open path requires a live
 * {@code ServerPlayer} (covered by gametest / in-game smoke), but the null-input
 * and not-loaded guards can be validated deterministically here.
 */
class IronInscriptionOpenerServiceTest {
    @Test
    void null_player_returns_false_and_does_not_throw() {
        assertFalse(IronInscriptionOpenerService.openVirtual(null));
    }
}
