package tong.statmod.effects;

import java.util.Optional;
import tong.statmod.progression.xp.WeaponClassification;
import tong.statmod.stats.StatProgress;
import tong.statmod.stats.StatType;

public final class CombatStatScaling {
    private CombatStatScaling() {
    }

    public static Optional<StatType> offensiveStat(WeaponClassification classification) {
        if (classification == null) {
            return Optional.empty();
        }
        return switch (classification) {
            case HEAVY -> Optional.of(StatType.BRUTE_FORCE);
            case BLADE -> Optional.of(StatType.BLADE_TECHNIQUE);
            case PRECISION -> Optional.of(StatType.PRECISION);
            case AMBIGUOUS, UNCLASSIFIED -> Optional.empty();
        };
    }

    public static double offensiveMultiplier(int level, CombatScalingRules rules) {
        return offensiveMultiplier(level, 0.0, rules);
    }

    public static double offensiveMultiplier(
            int level, double perkBonus, CombatScalingRules rules) {
        int bounded = Math.max(0, Math.min(StatProgress.MAX_LEVEL, level));
        double progress = bounded / (double) StatProgress.MAX_LEVEL;
        double multiplier = rules.weaponDamageBase()
                + rules.weaponDamageScale() * Math.pow(progress, rules.weaponDamageExponent());
        double continuous = Double.isFinite(multiplier) && multiplier >= 0.0 ? multiplier : 1.0;
        return continuous * (1.0 + boundedPerk(perkBonus));
    }

    public static double defensiveMultiplier(
            int resistanceLevel, int enduranceLevel, CombatScalingRules rules) {
        return defensiveMultiplier(resistanceLevel, enduranceLevel, 0.0, rules);
    }

    public static double defensiveMultiplier(int resistanceLevel, int enduranceLevel,
            double perkBonus, CombatScalingRules rules) {
        double resistance = Math.min(0.95,
                rules.physicalResistanceCap() * normalized(resistanceLevel)
                        + boundedPerk(perkBonus));
        double endurance = rules.physicalEnduranceCap() * normalized(enduranceLevel);
        double multiplier = (1.0 - resistance) * (1.0 - endurance);
        return Double.isFinite(multiplier) ? Math.max(0.0, multiplier) : 1.0;
    }

    public static float applyMultiplier(float amount, double multiplier) {
        if (!Float.isFinite(amount) || amount <= 0F
                || !Double.isFinite(multiplier) || multiplier < 0.0) {
            return amount;
        }
        double scaled = amount * multiplier;
        if (!Double.isFinite(scaled) || scaled >= Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        return (float) Math.max(0.0, scaled);
    }

    private static double normalized(int level) {
        int bounded = Math.max(0, Math.min(StatProgress.MAX_LEVEL, level));
        return bounded / (double) StatProgress.MAX_LEVEL;
    }

    private static double boundedPerk(double perkBonus) {
        return Double.isFinite(perkBonus)
                ? Math.max(0.0, Math.min(0.75, perkBonus))
                : 0.0;
    }
}
