package tong.statmod.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tong.statmod.capability.PlayerStats;
import tong.statmod.weapon.WeaponMasteryManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientStatsCacheTest {

    @BeforeEach
    void resetCaches() {
        ClientStatsCache.updateAll(new int[PlayerStats.STAT_COUNT], new int[PlayerStats.STAT_COUNT]);
        ClientStatsCache.updatePerks(new int[0], 0);
        ClientStatsCache.updateWeaponMastery(
            new int[WeaponMasteryManager.WEAPON_COUNT],
            new int[WeaponMasteryManager.WEAPON_COUNT]
        );
    }

    @Test
    void updatePerks_copiesInputArray() {
        int[] source = {1, 2, 3};

        ClientStatsCache.updatePerks(source, 4);
        source[0] = 99;

        assertArrayEquals(new int[] {1, 2, 3}, ClientStatsCache.getPerkIds());
    }

    @Test
    void getPerkIds_returnsDefensiveCopy() {
        ClientStatsCache.updatePerks(new int[] {4, 5}, 1);

        int[] exported = ClientStatsCache.getPerkIds();
        exported[0] = 99;

        assertArrayEquals(new int[] {4, 5}, ClientStatsCache.getPerkIds());
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
