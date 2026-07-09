package tong.statmod.progression;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RaceEffectApplierTest {

    @Test
    void scaleXpWithNullPlayerAndSoulLevelZero() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(0);
        // soulMult = 1.0 + 0/100 = 1.0, xpMult = 1.0 (null player)
        assertEquals(100, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));
    }

    @Test
    void scaleXpWithNullPlayerAndSoulLevelFifty() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(50);
        // soulMult = 1.0 + 50/200 = 1.25
        assertEquals(125, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));
    }

    @Test
    void scaleXpWithNullPlayerAndSoulLevelOneHundred() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(100);
        // soulMult = 1.0 + 100/200 = 1.5
        assertEquals(150, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));
    }

    @Test
    void scaleXpWithNullPlayerAndIntermediateSoulLevels() {
        PlayerStatData data = new PlayerStatData();

        // 1/200 = 0.005 est inexact en IEEE 754 → 100 * 1.004999... < 100.5
        data.setSoulLevel(1);
        assertEquals(100, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));

        // 25/200 = 0.125 exact → 112.5 exact
        data.setSoulLevel(25);
        assertEquals(113, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));

        // 99/200 = 0.495 inexact → peut donner 149 ou 150
        data.setSoulLevel(99);
        int actual = RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false);
        boolean ok = actual == 149 || actual == 150;
        org.junit.jupiter.api.Assertions.assertTrue(ok,
                "Expected 149 or 150 but got " + actual);
    }

    @Test
    void scaleXpClampedToMinimumOne() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(0);
        assertEquals(1, RaceEffectApplier.scaleXpAmount(null, 0, 0, data, false));
        assertEquals(1, RaceEffectApplier.scaleXpAmount(null, 0, -5, data, false));
    }

    @Test
    void scaleXpPreservesBaseXpScaling() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(0);
        assertEquals(10, RaceEffectApplier.scaleXpAmount(null, 0, 10, data, false));
        assertEquals(50, RaceEffectApplier.scaleXpAmount(null, 0, 50, data, false));
        assertEquals(1, RaceEffectApplier.scaleXpAmount(null, 0, 1, data, false));
    }

    @Test
    void scaleXpRoundsCorrectly() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(50);
        // 33 * 1.25 = 41.25 → round → 41
        assertEquals(41, RaceEffectApplier.scaleXpAmount(null, 0, 33, data, false));
    }
}
