package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CombatScalingRulesTest {
    @Test
    void exposesApprovedDefaults() {
        CombatScalingRules rules = CombatScalingRules.defaults();

        assertEquals(1.0, rules.weaponDamageBase());
        assertEquals(9.0, rules.weaponDamageScale());
        assertEquals(1.5, rules.weaponDamageExponent());
        assertEquals(0.65, rules.physicalResistanceCap());
        assertEquals(0.35, rules.physicalEnduranceCap());
    }

    @Test
    void replacesNonFiniteValuesAndClampsEveryRange() {
        CombatScalingRules rules = new CombatScalingRules(
                Double.NaN, 500.0, 0.0, 2.0, -1.0);

        assertEquals(1.0, rules.weaponDamageBase());
        assertEquals(99.0, rules.weaponDamageScale());
        assertEquals(0.1, rules.weaponDamageExponent());
        assertEquals(0.95, rules.physicalResistanceCap());
        assertEquals(0.0, rules.physicalEnduranceCap());
    }
}
