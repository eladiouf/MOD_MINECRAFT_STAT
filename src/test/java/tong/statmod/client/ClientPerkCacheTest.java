package tong.statmod.client;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    void nullAndNegativePayloadValuesAreSanitizedForUiSafety() {
        assertDoesNotThrow(() -> ClientPerkCache.update(null, new int[]{-3, 2}));
        assertEquals(0, ClientPerkCache.getPointsForFamily(0));
        assertEquals(2, ClientPerkCache.getPointsForFamily(1));

        assertDoesNotThrow(() -> ClientPerkCache.update(new int[]{-1, 999999}, null));
        assertEquals(0, ClientPerkCache.getPointsForFamily(0));
    }
}
