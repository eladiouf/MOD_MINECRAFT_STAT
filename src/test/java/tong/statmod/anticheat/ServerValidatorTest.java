package tong.statmod.anticheat;

import org.junit.jupiter.api.Test;
import tong.statmod.capability.PlayerStats;

import static org.junit.jupiter.api.Assertions.*;

class ServerValidatorTest {

    @Test
    void validateStatIndex_rejectsNegativeIndex() {
        assertFalse(ServerValidator.isValidStatIndex(-1), "Index -1 doit être rejeté");
    }

    @Test
    void validateStatIndex_rejectsIndexAtStatCount() {
        assertFalse(ServerValidator.isValidStatIndex(PlayerStats.STAT_COUNT),
            "Index == STAT_COUNT doit être rejeté");
    }

    @Test
    void validateStatIndex_acceptsZero() {
        assertTrue(ServerValidator.isValidStatIndex(0));
    }

    @Test
    void validateStatIndex_acceptsLastValidIndex() {
        assertTrue(ServerValidator.isValidStatIndex(PlayerStats.STAT_COUNT - 1));
    }
}
