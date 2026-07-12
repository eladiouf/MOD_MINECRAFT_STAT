package tong.statmod.client;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientCacheResetTest {
    @Test
    void resetClearsClientCachesBackToSafeDefaults() {
        ClientStatCache.updateAll(new int[]{5, 9}, new int[]{12, 18}, 7, 44, 12);
        ClientPerkCache.update(new int[]{Perk.BRUTE_CORE.id}, new int[]{3, 2, 1, 0, 0});
        ClientStaminaCache.update(125.0f, 8.0f, true);
        ClientMagicCache.update(
                new String[]{"fire/t1"},
                new String[]{"irons_spellbooks:magic_missile"},
                6,
                new int[]{4, 3, 2, 1},
                new int[]{9, 8, 7, 6},
                2,
                1);

        ClientStatCache.reset();
        ClientPerkCache.reset();
        ClientStaminaCache.reset();
        ClientMagicCache.reset();

        assertEquals(0, ClientStatCache.getLevel(0));
        assertEquals(0, ClientStatCache.getXp(0));
        assertEquals(0, ClientStatCache.getSoulLevel());
        assertEquals(0, ClientStatCache.getDungeonPoints());
        assertEquals(1, ClientStatCache.getDungeonFloorReached());
        assertFalse(ClientPerkCache.isUnlocked(Perk.BRUTE_CORE));
        assertEquals(0, ClientPerkCache.getPointsForFamily(0));
        assertEquals(0.0f, ClientStaminaCache.getCurrentStamina(), 0.0001f);
        assertEquals(0.0f, ClientStaminaCache.getFatigueDebt(), 0.0001f);
        assertFalse(ClientStaminaCache.isMeditating());
        assertEquals(0, ClientMagicCache.getMagicPoints());
        assertEquals(0, ClientMagicCache.getMagicNodeCount());
        assertEquals(0, ClientMagicCache.getLearnedSpellsCount());
        assertEquals(0, ClientMagicCache.getMasteryProgress(MagicBranch.FIRE));
        assertEquals(0, ClientMagicCache.getPracticeMasteryProgress(MagicBranch.FIRE));
        assertEquals(-1, ClientMagicCache.getRaceOrdinal());
        assertEquals(-1, ClientMagicCache.getStartBranchOrdinal());
    }
}
