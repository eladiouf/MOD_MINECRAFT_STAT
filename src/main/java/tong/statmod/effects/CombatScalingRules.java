package tong.statmod.effects;

public record CombatScalingRules(
        double weaponDamageBase,
        double weaponDamageScale,
        double weaponDamageExponent,
        double physicalResistanceCap,
        double physicalEnduranceCap) {

    public CombatScalingRules {
        weaponDamageBase = finiteClamp(weaponDamageBase, 1.0, 1.0, 10.0);
        weaponDamageScale = finiteClamp(weaponDamageScale, 9.0, 0.0, 99.0);
        weaponDamageExponent = finiteClamp(weaponDamageExponent, 1.5, 0.1, 5.0);
        physicalResistanceCap = finiteClamp(physicalResistanceCap, 0.65, 0.0, 0.95);
        physicalEnduranceCap = finiteClamp(physicalEnduranceCap, 0.35, 0.0, 0.95);
    }

    public static CombatScalingRules defaults() {
        return new CombatScalingRules(1.0, 9.0, 1.5, 0.65, 0.35);
    }

    private static double finiteClamp(
            double value, double fallback, double minimum, double maximum) {
        double finite = Double.isFinite(value) ? value : fallback;
        return Math.max(minimum, Math.min(maximum, finite));
    }
}
