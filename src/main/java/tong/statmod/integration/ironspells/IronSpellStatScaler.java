package tong.statmod.integration.ironspells;

import tong.statmod.magic.MagicBranch;

final class IronSpellStatScaler {
    private static final double MAX_MANA_PER_LEVEL = 15.0d;
    private static final double BASE_MANA_FLAT = 400.0d;
    // Iron's native regen is cancelled (MANA_REGEN = 0 via -0.1 modifier).
    // Our onPlayerTickRegen provides absolute regen (mana/sec).
    private static final double BASE_MANA_REGEN = 1.0000d;
    private static final double MANA_REGEN_PER_MANA_POOL = 0.1400d;
    private static final double SPELL_POWER_PER_ARCANE = 0.0015d;
    private static final double ELEMENTAL_POWER_PER_AFFINITY = 0.0015d;
    private static final double SPELL_RESIST_PER_LEVEL = 0.003d;
    private static final double CAST_TIME_REDUCTION_PER_LEVEL = 0.003d;
    private static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.004d;
    private static final double CORE_PERCENT_BONUS = 0.05d;
    private static final double CORE_MANA_BONUS = 50.0d;
    private IronSpellStatScaler() {}

    static double maxManaBonus(int manaPoolLevel) {
        return maxManaBonus(manaPoolLevel, false);
    }

    static double maxManaBonus(int manaPoolLevel, boolean manaPoolCoreUnlocked) {
        return BASE_MANA_FLAT + Math.max(0, manaPoolLevel) * MAX_MANA_PER_LEVEL
                + (manaPoolCoreUnlocked ? CORE_MANA_BONUS : 0.0d);
    }

    static double manaRegenBonus(int manaPoolLevel) {
        return BASE_MANA_REGEN
                + Math.max(0, manaPoolLevel) * MANA_REGEN_PER_MANA_POOL;
    }

    static double spellPowerBonus(int arcanePowerLevel) {
        return spellPowerBonus(arcanePowerLevel, false);
    }

    static double spellPowerBonus(int arcanePowerLevel, boolean arcaneCoreUnlocked) {
        return Math.max(0, arcanePowerLevel) * SPELL_POWER_PER_ARCANE
                + coreBonus(arcaneCoreUnlocked);
    }

    static double elementalSpellPowerBonus(MagicBranch branch,
                                           int fireAffinity,
                                           int waterAffinity,
                                           int earthAffinity,
                                           int airAffinity) {
        return elementalSpellPowerBonus(
                branch, fireAffinity, waterAffinity, earthAffinity, airAffinity,
                false, false, false, false);
    }

    static double elementalSpellPowerBonus(MagicBranch branch,
                                           int fireAffinity,
                                           int waterAffinity,
                                           int earthAffinity,
                                           int airAffinity,
                                           boolean fireCoreUnlocked,
                                           boolean waterCoreUnlocked,
                                           boolean earthCoreUnlocked,
                                           boolean airCoreUnlocked) {
        if (branch == null) {
            return 0.0d;
        }
        int level = switch (branch) {
            case FIRE -> fireAffinity;
            case WATER -> waterAffinity;
            case EARTH -> earthAffinity;
            case AIR -> airAffinity;
            default -> 0;
        };
        boolean coreUnlocked = switch (branch) {
            case FIRE -> fireCoreUnlocked;
            case WATER -> waterCoreUnlocked;
            case EARTH -> earthCoreUnlocked;
            case AIR -> airCoreUnlocked;
            default -> false;
        };
        return Math.max(0, level) * ELEMENTAL_POWER_PER_AFFINITY
                + coreBonus(coreUnlocked);
    }

    static double spellResistBonus(int magicResistanceLevel) {
        return spellResistBonus(magicResistanceLevel, false);
    }

    static double spellResistBonus(int magicResistanceLevel, boolean magicResistanceCoreUnlocked) {
        return Math.max(0, magicResistanceLevel) * SPELL_RESIST_PER_LEVEL
                + coreBonus(magicResistanceCoreUnlocked);
    }

    static double castTimeReductionBonus(int castingSpeedLevel) {
        return castTimeReductionBonus(castingSpeedLevel, false);
    }

    static double castTimeReductionBonus(int castingSpeedLevel, boolean castingSpeedCoreUnlocked) {
        return Math.max(0, castingSpeedLevel) * CAST_TIME_REDUCTION_PER_LEVEL
                + coreBonus(castingSpeedCoreUnlocked);
    }

    static double cooldownReductionBonus(int eruditionLevel) {
        return cooldownReductionBonus(eruditionLevel, false);
    }

    static double cooldownReductionBonus(int eruditionLevel, boolean eruditionCoreUnlocked) {
        return Math.max(0, eruditionLevel) * COOLDOWN_REDUCTION_PER_LEVEL
                + coreBonus(eruditionCoreUnlocked);
    }

    private static double coreBonus(boolean unlocked) {
        return unlocked ? CORE_PERCENT_BONUS : 0.0d;
    }
}
