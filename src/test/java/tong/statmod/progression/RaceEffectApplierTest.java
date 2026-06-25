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
        // soulMult = 1.0 + 50/100 = 1.5
        assertEquals(150, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));
    }

    @Test
    void scaleXpWithNullPlayerAndSoulLevelOneHundred() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(100);
        // soulMult = 1.0 + 100/100 = 2.0
        assertEquals(200, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));
    }

    @Test
    void scaleXpWithNullPlayerAndIntermediateSoulLevels() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(1);
        assertEquals(101, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));

        data.setSoulLevel(25);
        assertEquals(125, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));

        data.setSoulLevel(99);
        assertEquals(199, RaceEffectApplier.scaleXpAmount(null, 0, 100, data, false));
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
        // 33 * 1.5 = 49.5 → round → 50
        assertEquals(50, RaceEffectApplier.scaleXpAmount(null, 0, 33, data, false));
        // 33 * 1.01 (soul 1) = 33.33 → round → 33
        data.setSoulLevel(1);
        assertEquals(33, RaceEffectApplier.scaleXpAmount(null, 0, 33, data, false));
    }
}
