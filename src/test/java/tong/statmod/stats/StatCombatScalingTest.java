package tong.statmod.stats;

import org.junit.jupiter.api.Test;
import tong.statmod.config.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatCombatScalingTest {
    private static final double EPSILON = 1.0e-6d;
    private static final float DEFAULT_BASE = (float) Config.DEFAULT_WEAPON_DAMAGE_BASE;
    private static final float DEFAULT_SCALE = (float) Config.DEFAULT_WEAPON_DAMAGE_SCALE;

    @Test
    void primaryWeaponStatsReachTenAtLevelOneHundred() {
        assertEquals(10.0f, StatCombatScaling.weaponDamageMultiplier(
                StatType.BRUTE_FORCE, 100, 0, 0, 0, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
        assertEquals(10.0f, StatCombatScaling.weaponDamageMultiplier(
                StatType.BLADE_TECHNIQUE, 0, 100, 0, 0, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
        assertEquals(10.0f, StatCombatScaling.weaponDamageMultiplier(
                StatType.PRECISION, 0, 0, 100, 0, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
    }

    @Test
    void primaryCurveAcceleratesThroughApprovedMilestones() {
        assertEquals(2.5625f, primaryDamageAt(25), EPSILON);
        assertEquals(4.5052037f, primaryDamageAt(50), EPSILON);
        assertEquals(7.020912f, primaryDamageAt(75), EPSILON);
    }

    @Test
    void fastAndArcaneWeaponsKeepReducedRawDamageWeight() {
        assertEquals(6.6f, StatCombatScaling.weaponDamageMultiplier(
                StatType.RAPIDITE, 0, 0, 0, 100, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
        assertEquals(6.6f, StatCombatScaling.weaponDamageMultiplier(
                StatType.ARCANE_POWER, 0, 0, 0, 0, 100, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
    }

    @Test
    void levelZeroGivesBaseMultiplier() {
        assertEquals(1.50f, StatCombatScaling.weaponDamageMultiplier(
                StatType.BRUTE_FORCE, 0, 0, 0, 0, 0, DEFAULT_BASE, DEFAULT_SCALE), EPSILON);
    }

    @Test
    void intimidationOnlyBoostsMarkedTargets() {
        assertEquals(1.0f, StatCombatScaling.intimidationDamageMultiplier(100, false), EPSILON);
        // Balance 30j : +0.5% dmg par level sur cible marquée au lieu de 0.3% (au niveau 100 -> 1.50f)
        assertEquals(1.50f, StatCombatScaling.intimidationDamageMultiplier(100, true), EPSILON);
    }

    @Test
    void defensiveMultipliersAreClampedAndSeparatedByRole() {
        assertEquals(0.35f, StatCombatScaling.physicalDamageTakenMultiplier(100), EPSILON);
        assertEquals(0.35f, StatCombatScaling.physicalDamageTakenMultiplier(500), EPSILON);
        assertEquals(0.35f, StatCombatScaling.magicDamageTakenMultiplier(100), EPSILON);
        assertEquals(0.35f, StatCombatScaling.magicDamageTakenMultiplier(500), EPSILON);
        assertEquals(0.65f, StatCombatScaling.enduranceDamageTakenMultiplier(100), EPSILON);
        assertEquals(0.65f, StatCombatScaling.enduranceDamageTakenMultiplier(500), EPSILON);
        assertEquals(0.55f, StatCombatScaling.statusDamageTakenMultiplier(100), EPSILON);
        assertEquals(0.55f, StatCombatScaling.statusDamageTakenMultiplier(500), EPSILON);
    }

    @Test
    void incomingDamageMultiplierOnlyAppliesTheMatchingDefensiveStats() {
        assertEquals(0.2275f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.PHYSICAL, 100, 100, 100, 100), EPSILON);
        assertEquals(0.35f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.MAGIC, 100, 100, 100, 100), EPSILON);
        assertEquals(0.55f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.STATUS, 100, 100, 100, 100), EPSILON);
        assertEquals(1.0f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.ENVIRONMENT, 100, 100, 100, 100), EPSILON);
    }

    @Test
    void damageRoleSeparatesProjectilesMagicStatusAndEnvironment() {
        assertEquals(StatCombatScaling.IncomingDamageRole.PHYSICAL,
                StatCombatScaling.damageRole(false, false, "arrow"));
        assertEquals(StatCombatScaling.IncomingDamageRole.MAGIC,
                StatCombatScaling.damageRole(true, false, "magic"));
        assertEquals(StatCombatScaling.IncomingDamageRole.MAGIC,
                StatCombatScaling.damageRole(false, true, "indirectMagic"));
        assertEquals(StatCombatScaling.IncomingDamageRole.STATUS,
                StatCombatScaling.damageRole(false, false, "wither"));
        assertEquals(StatCombatScaling.IncomingDamageRole.ENVIRONMENT,
                StatCombatScaling.damageRole(false, false, "fall"));
    }

    @Test
    void willpowerReducesFiniteNegativeEffectDurationsButNotInfiniteOnes() {
        assertEquals(1100, StatCombatScaling.negativeEffectDurationTicks(2000, 100, false));
        assertEquals(900, StatCombatScaling.negativeEffectDurationTicks(2000, 100, true));
        assertEquals(900, StatCombatScaling.negativeEffectDurationTicks(2000, 500, true));
        assertEquals(-1, StatCombatScaling.negativeEffectDurationTicks(-1, 100, true));
        assertEquals(0, StatCombatScaling.negativeEffectDurationTicks(0, 100, true));
    }

    private static float primaryDamageAt(int level) {
        return StatCombatScaling.weaponDamageMultiplier(
                StatType.BRUTE_FORCE, level, 0, 0, 0, 0, DEFAULT_BASE, DEFAULT_SCALE);
    }
}
