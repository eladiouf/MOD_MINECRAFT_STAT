package tong.statmod.economy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FdpDenominationTest {
    @Test
    void exposesTheEightExactValuesInAscendingOrder() {
        assertEquals(List.of(50L, 100L, 200L, 500L, 1000L, 2000L, 5000L, 10000L),
                FdpDenomination.ascending().stream().map(FdpDenomination::value).toList());
    }

    @Test
    void identifiersAreUniqueAndMatchCoinOrNoteKind() {
        var ids = FdpDenomination.ascending().stream().map(FdpDenomination::id).toList();
        assertEquals(ids.size(), ids.stream().distinct().count());
        assertEquals("fdp_coin_50", ids.getFirst());
        assertEquals("fdp_note_10000", ids.getLast());
    }
}
