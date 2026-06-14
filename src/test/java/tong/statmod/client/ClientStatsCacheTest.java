package tong.statmod.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tong.statmod.capability.PlayerStats;
import tong.statmod.perks.Perk;
import tong.statmod.weapon.WeaponMasteryManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientStatsCacheTest {

    @BeforeEach
    void resetCaches() {
        ClientStatsCache.updateAll(new int[PlayerStats.STAT_COUNT], new int[PlayerStats.STAT_COUNT]);
        ClientPerkCache.update(new int[0], new int[23]);
        ClientStatsCache.updateWeaponMastery(
            new int[WeaponMasteryManager.WEAPON_COUNT],
            new int[WeaponMasteryManager.WEAPON_COUNT]
        );
    }

    @Test
    void updatePerks_copiesInputArray() {
        int[] source = {Perk.BRUTE_CORE.id, Perk.BLADE_CORE.id, Perk.RAPID_CORE.id};

        ClientPerkCache.update(source, new int[23]);
        source[0] = 99;

        assertTrue(ClientPerkCache.isUnlocked(Perk.BRUTE_CORE));
        assertTrue(ClientPerkCache.isUnlocked(Perk.BLADE_CORE));
        assertTrue(ClientPerkCache.isUnlocked(Perk.RAPID_CORE));
    }

    @Test
    void updateAll_clearsTailWhenPayloadShrinks() {
        ClientStatsCache.updateAll(new int[] {10, 20, 30}, new int[] {1, 2, 3});

        ClientStatsCache.updateAll(new int[] {7}, new int[] {9});

        assertEquals(7, ClientStatsCache.getLevel(0));
        assertEquals(0, ClientStatsCache.getLevel(1));
        assertEquals(0, ClientStatsCache.getXp(2));
    }

    @Test
    void updateWeaponMastery_clearsTailWhenPayloadShrinks() {
        ClientStatsCache.updateWeaponMastery(new int[] {6, 5, 4}, new int[] {3, 2, 1});

        ClientStatsCache.updateWeaponMastery(new int[] {8}, new int[] {9});

        assertEquals(8, ClientStatsCache.getWeaponLevel(0));
        assertEquals(0, ClientStatsCache.getWeaponLevel(1));
        assertEquals(0, ClientStatsCache.getWeaponXp(2));
    }
}
