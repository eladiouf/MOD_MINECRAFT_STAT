package tong.statmod.network;

import tong.statmod.capability.PlayerStats;
import tong.statmod.perks.Perk;
import tong.statmod.weapon.WeaponMasteryManager;

public final class NetworkPayloadRules {
    public static final int MAX_STAT_COUNT = PlayerStats.STAT_COUNT;
    public static final int MAX_PERK_COUNT = Perk.values().length;
    public static final int MAX_WEAPON_COUNT = WeaponMasteryManager.WEAPON_COUNT;
    public static final int MAX_STAT_UPDATE_VALUES = PlayerStats.STAT_COUNT * 3;

    private NetworkPayloadRules() {
    }

    public static int[] requireStatArray(int[] values, String fieldName) {
        if (values.length > MAX_STAT_COUNT) {
            throw new IllegalArgumentException(fieldName + " exceeds max stat payload size: " + values.length);
        }
        return values;
    }

    public static int[] requirePerkArray(int[] values, String fieldName) {
        if (values.length > MAX_PERK_COUNT) {
            throw new IllegalArgumentException(fieldName + " exceeds max perk payload size: " + values.length);
        }
        return values;
    }

    public static int[] requireWeaponArray(int[] values, String fieldName) {
        if (values.length > MAX_WEAPON_COUNT) {
            throw new IllegalArgumentException(fieldName + " exceeds max weapon payload size: " + values.length);
        }
        return values;
    }

    public static int[] requireStatUpdateValues(int[] values) {
        if (values.length > MAX_STAT_UPDATE_VALUES) {
            throw new IllegalArgumentException("stat update payload exceeds max size: " + values.length);
        }
        if (values.length % 3 != 0) {
            throw new IllegalArgumentException("stat update payload must contain index/level/xp triplets");
        }
        return values;
    }

    public static void requireMatchingStatArrays(int[] levels, int[] xp, String label) {
        if (levels.length != xp.length) {
            throw new IllegalArgumentException(label + " levels/xp length mismatch: " + levels.length + " != " + xp.length);
        }
    }

    public static void requireMatchingWeaponArrays(int[] levels, int[] xp, String label) {
        if (levels.length != xp.length) {
            throw new IllegalArgumentException(label + " levels/xp length mismatch: " + levels.length + " != " + xp.length);
        }
    }
}
