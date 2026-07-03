package tong.statmod.integration.ironspells;

final class IronSpellStatScaler {
    private static final double MAX_MANA_PER_LEVEL = 1.0d;
    private static final double MANA_REGEN_PER_MANA_POOL = 0.005d;
    private static final double MANA_REGEN_PER_ERUDITION = 0.005d;
    private static final double SPELL_POWER_PER_ARCANE = 0.003d;
    private static final double SPELL_RESIST_PER_LEVEL = 0.003d;
    private static final double CAST_TIME_REDUCTION_PER_LEVEL = 0.003d;
    private static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.004d;

    private IronSpellStatScaler() {}

    static double maxManaBonus(int manaPoolLevel) {
        return Math.max(0, manaPoolLevel) * MAX_MANA_PER_LEVEL;
    }

    static double manaRegenBonus(int manaPoolLevel, int eruditionLevel) {
        return Math.max(0, manaPoolLevel) * MANA_REGEN_PER_MANA_POOL
                + Math.max(0, eruditionLevel) * MANA_REGEN_PER_ERUDITION;
    }

    static double spellPowerBonus(int arcanePowerLevel) {
        return Math.max(0, arcanePowerLevel) * SPELL_POWER_PER_ARCANE;
    }

    static double spellResistBonus(int magicResistanceLevel) {
        return Math.max(0, magicResistanceLevel) * SPELL_RESIST_PER_LEVEL;
    }

    static double castTimeReductionBonus(int castingSpeedLevel) {
        return Math.max(0, castingSpeedLevel) * CAST_TIME_REDUCTION_PER_LEVEL;
    }

    static double cooldownReductionBonus(int eruditionLevel) {
        return Math.max(0, eruditionLevel) * COOLDOWN_REDUCTION_PER_LEVEL;
    }
}
