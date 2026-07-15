package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tong.statmod.progression.xp.WeaponClassification;
import tong.statmod.stats.StatType;

class CombatStatScalingTest {
    private static final double EPSILON = 0.000_001;

    @Test
    void followsExactDefaultOffensiveAnchors() {
        CombatScalingRules rules = CombatScalingRules.defaults();

        assertEquals(1.0, CombatStatScaling.offensiveMultiplier(0, rules), EPSILON);
        assertEquals(2.125, CombatStatScaling.offensiveMultiplier(25, rules), EPSILON);
        assertEquals(4.181980515, CombatStatScaling.offensiveMultiplier(50, rules), EPSILON);
        assertEquals(6.845671476, CombatStatScaling.offensiveMultiplier(75, rules), EPSILON);
        assertEquals(10.0, CombatStatScaling.offensiveMultiplier(100, rules), EPSILON);
    }

    @Test
    void clampsLevelsAndMapsOnlySupportedClassifications() {
        CombatScalingRules rules = CombatScalingRules.defaults();

        assertEquals(1.0, CombatStatScaling.offensiveMultiplier(-20, rules), EPSILON);
        assertEquals(10.0, CombatStatScaling.offensiveMultiplier(200, rules), EPSILON);
        assertEquals(StatType.BRUTE_FORCE,
                CombatStatScaling.offensiveStat(WeaponClassification.HEAVY).orElseThrow());
        assertEquals(StatType.BLADE_TECHNIQUE,
                CombatStatScaling.offensiveStat(WeaponClassification.BLADE).orElseThrow());
        assertEquals(StatType.PRECISION,
                CombatStatScaling.offensiveStat(WeaponClassification.PRECISION).orElseThrow());
        assertTrue(CombatStatScaling.offensiveStat(WeaponClassification.AMBIGUOUS).isEmpty());
        assertTrue(CombatStatScaling.offensiveStat(WeaponClassification.UNCLASSIFIED).isEmpty());
        assertTrue(CombatStatScaling.offensiveStat(null).isEmpty());
    }

    @Test
    void appliesOnlyFiniteBoundedOffensivePerkBonuses() {
        CombatScalingRules rules = CombatScalingRules.defaults();
        double continuous = CombatStatScaling.offensiveMultiplier(50, rules);

        assertEquals(continuous,
                CombatStatScaling.offensiveMultiplier(50, 0.0, rules), EPSILON);
        assertEquals(continuous * 1.05,
                CombatStatScaling.offensiveMultiplier(50, 0.05, rules), EPSILON);
        assertEquals(continuous * 1.15,
                CombatStatScaling.offensiveMultiplier(50, 0.15, rules), EPSILON);
        assertEquals(continuous,
                CombatStatScaling.offensiveMultiplier(50, -0.50, rules), EPSILON);
        assertEquals(continuous,
                CombatStatScaling.offensiveMultiplier(50, Double.NaN, rules), EPSILON);
        assertEquals(continuous * 1.75,
                CombatStatScaling.offensiveMultiplier(50, 50.0, rules), EPSILON);
    }

    @Test
    void appliesBoundedMultiplicativePhysicalDefense() {
        CombatScalingRules rules = CombatScalingRules.defaults();

        assertEquals(1.0, CombatStatScaling.defensiveMultiplier(0, 0, rules), EPSILON);
        assertEquals(0.2275, CombatStatScaling.defensiveMultiplier(100, 100, rules), EPSILON);
        assertEquals(22.75F, CombatStatScaling.applyMultiplier(100F,
                CombatStatScaling.defensiveMultiplier(100, 100, rules)), 0.0001F);
        assertEquals(0.2275, CombatStatScaling.defensiveMultiplier(500, 500, rules), EPSILON);
        assertEquals(1.0, CombatStatScaling.defensiveMultiplier(-20, -20, rules), EPSILON);
    }

    @Test
    void addsOnlyFinitePerkResistanceBeforeTheSafetyCap() {
        CombatScalingRules rules = CombatScalingRules.defaults();

        assertEquals((1.0 - 0.325) * (1.0 - 0.175),
                CombatStatScaling.defensiveMultiplier(50, 50, 0.0, rules), EPSILON);
        assertEquals((1.0 - 0.385) * (1.0 - 0.175),
                CombatStatScaling.defensiveMultiplier(50, 50, 0.06, rules), EPSILON);
        assertEquals(0.1885,
                CombatStatScaling.defensiveMultiplier(100, 100, 0.06, rules), EPSILON);
        assertEquals(CombatStatScaling.defensiveMultiplier(50, 50, rules),
                CombatStatScaling.defensiveMultiplier(50, 50, -0.20, rules), EPSILON);
        assertEquals(CombatStatScaling.defensiveMultiplier(50, 50, rules),
                CombatStatScaling.defensiveMultiplier(50, 50, Double.NaN, rules), EPSILON);
        assertEquals((1.0 - 0.95) * (1.0 - 0.175),
                CombatStatScaling.defensiveMultiplier(50, 50, 50.0, rules), EPSILON);
    }

    @Test
    void keepsInvalidDamageNeutralAndClampsOverflow() {
        assertTrue(Float.isNaN(CombatStatScaling.applyMultiplier(Float.NaN, 2.0)));
        assertEquals(-4F, CombatStatScaling.applyMultiplier(-4F, 2.0));
        assertEquals(5F, CombatStatScaling.applyMultiplier(5F, Double.NaN));
        assertEquals(Float.MAX_VALUE,
                CombatStatScaling.applyMultiplier(Float.MAX_VALUE, 10.0));
    }
}
