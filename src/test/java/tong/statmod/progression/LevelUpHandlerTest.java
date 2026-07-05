package tong.statmod.progression;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.PerkPointAllocator;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelUpHandlerTest {

    @Test
    void tierIsGlobalLevelDividedByTen() {
        assertEquals(0, 0 / 10);
        assertEquals(0, 9 / 10);
        assertEquals(1, 10 / 10);
        assertEquals(1, 19 / 10);
        assertEquals(2, 20 / 10);
        assertEquals(10, 100 / 10);
    }

    @Test
    void grantsCorrectNumberOfPointsForTierCrossing() {
        int globalLevel = 10;
        int tier = globalLevel / 10;
        int already = 0;
        int granted = tier - already;
        assertEquals(1, granted);
    }

    @Test
    void grantsMultiplePointsWhenSkippingTiers() {
        int globalLevel = 25;
        int tier = globalLevel / 10;
        int already = 1;
        int granted = tier - already;
        assertEquals(1, granted);
    }

    @Test
    void grantsTwoPointsWhenJumpingFromZeroToTwenty() {
        int globalLevel = 20;
        int tier = globalLevel / 10;
        int already = 0;
        int granted = tier - already;
        assertEquals(2, granted);
    }

    @Test
    void noGrantWhenTierNotChanged() {
        int globalLevel = 10;
        int tier = globalLevel / 10;
        int already = 1;
        int granted = tier - already;
        assertEquals(0, granted);
    }

    @Test
    void globalLevelComputedFromAverageStatLevel() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(0, data.getGlobalLevel());

        for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
            data.setLevel(i, 10);
        }
        assertEquals(10, data.getGlobalLevel());
    }

    @Test
    void globalLevelTenYieldsTierOne() {
        PlayerStatData data = new PlayerStatData();
        for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
            data.setLevel(i, 10);
        }
        assertEquals(1, data.getGlobalLevel() / 10);
    }

    @Test
    void perkPointAllocatorGrantsPointsToAllFamilies() {
        PlayerStatData data = new PlayerStatData();
        PerkPointAllocator.grantPointsToAllFamilies(data, 1);

        assertEquals(1, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(1, data.getPerkPointsForStat(StatType.BLADE_TECHNIQUE.index));
        assertEquals(1, data.getPerkPointsForStat(StatType.ARCANE_POWER.index));
        assertEquals(1, data.getPerkPointsForStat(StatType.WATER_AFFINITY.index));
        assertEquals(1, data.getPerkPointsForStat(StatType.INTIMIDATION.index));
        assertEquals(1, data.getPerkPointsForStat(StatType.FORGING.index));
    }

    @Test
    void perkPointAllocatorGrantsAccumulatedPoints() {
        PlayerStatData data = new PlayerStatData();
        PerkPointAllocator.grantPointsToAllFamilies(data, 2);

        assertEquals(2, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(2, data.getPerkPointsForStat(StatType.FORGING.index));
    }

    @Test
    void perkPointAllocatorSkipsNullData() {
        // Should not throw
        PerkPointAllocator.grantPointsToAllFamilies(null, 1);
        PerkPointAllocator.grantPointsToAllFamilies(new PlayerStatData(), 0);
        PerkPointAllocator.grantPointsToAllFamilies(new PlayerStatData(), -1);
    }

    @Test
    void grantPendingPerkTiersCreditsReachedTierAndPersistsMarker() {
        PlayerStatData data = new PlayerStatData();
        for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
            data.setLevel(i, 10);
        }

        int granted = LevelUpHandler.grantPendingPerkTiers(data);

        assertEquals(1, granted);
        assertEquals(1, data.getLastPerkGrantTier());
        assertEquals(1, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(1, data.getPerkPointsForStat(StatType.ARCANE_POWER.index));
    }

    @Test
    void grantPendingPerkTiersDoesNothingWhenTierAlreadyGranted() {
        PlayerStatData data = new PlayerStatData();
        for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
            data.setLevel(i, 10);
        }
        data.setLastPerkGrantTier(1);

        int granted = LevelUpHandler.grantPendingPerkTiers(data);

        assertEquals(0, granted);
        assertEquals(1, data.getLastPerkGrantTier());
        assertEquals(0, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
    }
}
