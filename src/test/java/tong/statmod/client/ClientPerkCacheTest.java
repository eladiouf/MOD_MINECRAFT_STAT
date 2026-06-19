package tong.statmod.client;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientPerkCacheTest {
    @Test
    void sharesFamilyPointsAcrossSiblingStats() {
        int[] familyPoints = new int[StatFamily.values().length];
        familyPoints[StatFamily.FRONTLINE_PHYSICAL_COMBAT.ordinal()] = 4;

        ClientPerkCache.update(new int[0], familyPoints);

        assertEquals(4, ClientPerkCache.getPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(4, ClientPerkCache.getPointsForStat(StatType.BLADE_TECHNIQUE.index));
    }

    @Test
    void tracksUnlockedPerksAcrossTheFullCanonicalCatalog() {
        int lastPerkId = Perk.values()[Perk.values().length - 1].id;
        Perk lastPerk = Perk.byId(lastPerkId);
        assertNotNull(lastPerk);

        ClientPerkCache.update(new int[]{lastPerkId}, new int[StatFamily.values().length]);

        assertTrue(ClientPerkCache.isUnlocked(lastPerk));
    }
}
