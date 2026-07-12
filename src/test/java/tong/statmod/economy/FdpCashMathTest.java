package tong.statmod.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FdpCashMathTest {
    @Test
    void breaksAmountIntoLargestDenominations() {
        var result = FdpCashMath.breakdown(18_850);
        assertEquals(1, result.get(10_000L));
        assertEquals(1, result.get(5_000L));
        assertEquals(1, result.get(2_000L));
        assertEquals(1, result.get(1_000L));
        assertEquals(1, result.get(500L));
        assertEquals(1, result.get(200L));
        assertEquals(1, result.get(100L));
        assertEquals(1, result.get(50L));
    }

    @Test
    void rejectsInvalidAmounts() {
        assertFalse(FdpCashMath.isWithdrawable(0));
        assertFalse(FdpCashMath.isWithdrawable(-50));
        assertFalse(FdpCashMath.isWithdrawable(125));
        assertTrue(FdpCashMath.isWithdrawable(150));
        assertThrows(IllegalArgumentException.class, () -> FdpCashMath.breakdown(125));
    }
}
