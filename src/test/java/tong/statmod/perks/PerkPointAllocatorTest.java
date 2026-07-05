package tong.statmod.perks;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerkPointAllocatorTest {
    @Test
    void grantsPointsOncePerFamily() {
        PlayerStatData data = new PlayerStatData();

        PerkPointAllocator.grantPointsToAllFamilies(data, 2);

        assertEquals(2, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(2, data.getPerkPointsForStat(StatType.ARCANE_POWER.index));
        assertEquals(2, data.getPerkPointsForStat(StatType.WATER_AFFINITY.index));
    }

    @Test
    void refundsOnlyPaidPerksIntoSharedFamilyPools() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);
        data.addUnlockedPerk(Perk.BLADE_CORE.id);
        data.markPerkFreeGranted(Perk.ARCANE_CORE.id);

        int refunded = PerkPointAllocator.refundPaidUnlockedPerks(data);

        assertEquals(2, refunded);
        assertEquals(2, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(2, data.getPerkPointsForStat(StatType.BLADE_TECHNIQUE.index));
        assertEquals(0, data.getPerkPointsForStat(StatType.ARCANE_POWER.index));
    }

    @Test
    void refundCannotBeMultipliedByDuplicatedPersistedPerks() {
        PlayerStatData data = new PlayerStatData();
        data.setUnlockedPerks(new int[]{Perk.BRUTE_CORE.id, Perk.BRUTE_CORE.id});

        int refunded = PerkPointAllocator.refundPaidUnlockedPerks(data);

        assertEquals(Perk.BRUTE_CORE.tier.cost, refunded);
        assertEquals(Perk.BRUTE_CORE.tier.cost,
                data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
    }
}
