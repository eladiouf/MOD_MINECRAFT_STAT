package tong.statmod.network;

import org.junit.jupiter.api.Test;
import tong.statmod.capability.PlayerStats;
import tong.statmod.perks.Perk;
import tong.statmod.weapon.WeaponMasteryManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkPayloadRulesTest {

    @Test
    void statArrays_acceptConfiguredStatCount() {
        assertDoesNotThrow(() ->
            NetworkPayloadRules.requireStatArray(new int[PlayerStats.STAT_COUNT], "levels"));
    }

    @Test
    void statArrays_rejectOversizedPayload() {
        assertThrows(IllegalArgumentException.class, () ->
            NetworkPayloadRules.requireStatArray(new int[PlayerStats.STAT_COUNT + 1], "levels"));
    }

    @Test
    void perkArrays_rejectOversizedPayload() {
        assertThrows(IllegalArgumentException.class, () ->
            NetworkPayloadRules.requirePerkArray(new int[Perk.values().length + 1], "perkIds"));
    }

    @Test
    void statUpdatePayload_requiresTriplets() {
        assertThrows(IllegalArgumentException.class, () ->
            NetworkPayloadRules.requireStatUpdateValues(new int[] {0, 12}));
    }

    @Test
    void statUpdatePayload_rejectsOversizedTriplets() {
        assertThrows(IllegalArgumentException.class, () ->
            NetworkPayloadRules.requireStatUpdateValues(new int[NetworkPayloadRules.MAX_STAT_UPDATE_VALUES + 3]));
    }

    @Test
    void matchingStatArrays_requireSameLength() {
        assertThrows(IllegalArgumentException.class, () ->
            NetworkPayloadRules.requireMatchingStatArrays(new int[PlayerStats.STAT_COUNT], new int[PlayerStats.STAT_COUNT - 1], "sync"));
    }

    @Test
    void weaponArrays_rejectOversizedPayload() {
        assertThrows(IllegalArgumentException.class, () ->
            NetworkPayloadRules.requireWeaponArray(new int[WeaponMasteryManager.WEAPON_COUNT + 1], "weaponLevels"));
    }
}
