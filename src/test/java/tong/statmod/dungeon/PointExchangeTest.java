package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PointExchangeTest {

    @Test
    void convertsClampedAmountAndFloorsCoins() {
        PointExchange.Result r = PointExchange.compute(50, 320, 1.0);
        assertEquals(50, r.converted());
        assertEquals(50, r.coins());
        assertEquals(270, r.remainingPoints());
    }

    @Test
    void clampsToAvailablePoints() {
        PointExchange.Result r = PointExchange.compute(999, 100, 1.0);
        assertEquals(100, r.converted());
        assertEquals(0, r.remainingPoints());
    }

    @Test
    void rejectsNegativeRequest() {
        PointExchange.Result r = PointExchange.compute(-5, 100, 1.0);
        assertEquals(0, r.converted());
        assertEquals(100, r.remainingPoints());
    }

    @Test
    void floorsCoinsWithFractionalRate() {
        PointExchange.Result r = PointExchange.compute(7, 100, 0.5);
        assertEquals(7, r.converted());
        assertEquals(3, r.coins());
    }
}
