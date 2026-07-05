package tong.statmod.perks;

import tong.statmod.stats.StatType;

public final class PerkCombatScaling {
    private PerkCombatScaling() {}

    private static boolean isWeaponFamilyStat(StatType stat) {
        return stat == StatType.BRUTE_FORCE
                || stat == StatType.BLADE_TECHNIQUE
                || stat == StatType.RAPIDITE
                || stat == StatType.PRECISION;
    }

    public static float coreWeaponDamageMultiplier(StatType weaponStat,
                                                   boolean bruteCoreUnlocked,
                                                   boolean bladeCoreUnlocked,
                                                   boolean precisionCoreUnlocked) {
        if (weaponStat == null) {
            return 1.0f;
        }
        return switch (weaponStat) {
            case BRUTE_FORCE -> bruteCoreUnlocked ? 1.05f : 1.0f;
            case BLADE_TECHNIQUE -> bladeCoreUnlocked ? 1.05f : 1.0f;
            case PRECISION -> precisionCoreUnlocked ? 1.05f : 1.0f;
            default -> 1.0f;
        };
    }

    public static boolean canUseWeaponFamilyPerk(StatType weaponStat, Perk perk) {
        if (perk == null || !isWeaponFamilyStat(perk.stat)) {
            return true;
        }
        return weaponStat == perk.stat;
    }

    public static float bladePostKillDamageMultiplier(StatType weaponStat,
                                                      boolean bladeTranscendenceUnlocked,
                                                      boolean postKillWindowActive) {
        return bladeTranscendenceUnlocked
                && postKillWindowActive
                && canUseWeaponFamilyPerk(weaponStat, Perk.BLADE_TRANSCENDENCE)
                ? 1.5f
                : 1.0f;
    }

    public static float bruteTranscendenceDamageMultiplier(StatType weaponStat,
                                                           boolean bruteTranscendenceUnlocked) {
        return bruteTranscendenceUnlocked
                && canUseWeaponFamilyPerk(weaponStat, Perk.BRUTE_TRANSCENDENCE)
                ? 1.5f
                : 1.0f;
    }

    public static float precisionMarkedProjectileDamageMultiplier(StatType weaponStat,
                                                                  boolean precisionSynergyUnlocked,
                                                                  boolean projectileDamage,
                                                                  boolean markedTarget) {
        return precisionSynergyUnlocked
                && projectileDamage
                && markedTarget
                && canUseWeaponFamilyPerk(weaponStat, Perk.PRECI_SYNERGY)
                ? 1.2f
                : 1.0f;
    }

    public static boolean canPierceSecondaryTarget(StatType weaponStat,
                                                   boolean precisionActiveUnlocked,
                                                   boolean projectileDamage) {
        return precisionActiveUnlocked
                && projectileDamage
                && canUseWeaponFamilyPerk(weaponStat, Perk.PRECI_ACTIVE);
    }
}
