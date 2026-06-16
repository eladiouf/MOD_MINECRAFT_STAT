package tong.statmod.integration;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RaceEffectApplierTest {

    @Test
    void scalesXpBySoulLevel() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(50);

        assertEquals(150, RaceEffectApplier.scaleXpAmount(null, 0, 100, data));
    }

    @Test
    void removeUnlockedPerkDropsOnlyMatchingEntry() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(1);
        data.addUnlockedPerk(2);
        data.addUnlockedPerk(3);

        data.removeUnlockedPerk(2);

        assertEquals(2, data.getUnlockedPerks().length);
        assertEquals(1, data.getUnlockedPerks()[0]);
        assertEquals(3, data.getUnlockedPerks()[1]);
    }
}
