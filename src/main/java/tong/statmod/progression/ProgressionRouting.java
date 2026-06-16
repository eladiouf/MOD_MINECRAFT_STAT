package tong.statmod.progression;

import tong.statmod.stats.StatType;

public final class ProgressionRouting {
    private ProgressionRouting() {}

    public static StatType combatStatForWeaponCategory(String weaponCategory) {
        return WeaponResolver.combatStatForWeaponCategory(weaponCategory);
    }

    public static StatType nonCombatStatFor(ActionType actionType) {
        return actionType == null ? null : actionType.primaryStat();
    }
}
