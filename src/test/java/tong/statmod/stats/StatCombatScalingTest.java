package tong.statmod.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatCombatScalingTest {
    private static final double EPSILON = 1.0e-6d;

    // base=1.5, scale=3.0 (défauts). Courbe : base + scale*weight*(lvl/100)^1.5.

    @Test
    void weaponDamageUsesOnlyTheResolvedWeaponStat() {
        // Niveau 100 → t=1 → base + scale*1 = 1.5 + 3.0 = 4.5 (stats primaires, weight 1.0).
        assertEquals(4.50f, StatCombatScaling.weaponDamageMultiplier(
                StatType.BRUTE_FORCE, 100, 100, 100, 100, 100, 1.5f, 3.0f), EPSILON);
        assertEquals(4.50f, StatCombatScaling.weaponDamageMultiplier(
                StatType.BLADE_TECHNIQUE, 100, 100, 100, 100, 100, 1.5f, 3.0f), EPSILON);
        assertEquals(4.50f, StatCombatScaling.weaponDamageMultiplier(
                StatType.PRECISION, 100, 100, 100, 100, 100, 1.5f, 3.0f), EPSILON);
    }

    @Test
    void fastAndArcaneWeaponsHaveTheirOwnLowerScaling() {
        // Niveau 100 → base + scale*0.6 = 1.5 + 1.8 = 3.3 (stats secondaires, weight 0.6).
        assertEquals(3.30f, StatCombatScaling.weaponDamageMultiplier(
                StatType.RAPIDITE, 100, 100, 100, 100, 100, 1.5f, 3.0f), EPSILON);
        assertEquals(3.30f, StatCombatScaling.weaponDamageMultiplier(
                StatType.ARCANE_POWER, 100, 100, 100, 100, 100, 1.5f, 3.0f), EPSILON);
    }

    @Test
    void levelZeroGivesBaseMultiplier() {
        // Niveau 0 → t=0 → base seul (1.5). Toutes les armes tapent déjà mieux qu'avant (×1.5).
        assertEquals(1.50f, StatCombatScaling.weaponDamageMultiplier(
                StatType.BRUTE_FORCE, 0, 0, 0, 0, 0, 1.5f, 3.0f), EPSILON);
    }

    @Test
    void intimidationOnlyBoostsMarkedTargets() {
        assertEquals(1.0f, StatCombatScaling.intimidationDamageMultiplier(100, false), EPSILON);
        assertEquals(1.30f, StatCombatScaling.intimidationDamageMultiplier(100, true), EPSILON);
    }

    @Test
    void defensiveMultipliersAreClampedAndSeparatedByRole() {
        assertEquals(0.50f, StatCombatScaling.physicalDamageTakenMultiplier(200), EPSILON);
        assertEquals(0.50f, StatCombatScaling.magicDamageTakenMultiplier(200), EPSILON);
        assertEquals(0.70f, StatCombatScaling.enduranceDamageTakenMultiplier(200), EPSILON);
        assertEquals(0.70f, StatCombatScaling.statusDamageTakenMultiplier(100), EPSILON);
    }

    @Test
    void incomingDamageMultiplierOnlyAppliesTheMatchingDefensiveStats() {
        assertEquals(0.35f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.PHYSICAL, 200, 200, 200, 100), EPSILON);
        assertEquals(0.50f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.MAGIC, 200, 200, 200, 100), EPSILON);
        assertEquals(0.70f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.STATUS, 200, 200, 200, 100), EPSILON);
        assertEquals(1.0f, StatCombatScaling.incomingDamageMultiplier(
                StatCombatScaling.IncomingDamageRole.ENVIRONMENT, 200, 200, 200, 100), EPSILON);
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
        assertEquals(1400, StatCombatScaling.negativeEffectDurationTicks(2000, 100, false));
        assertEquals(1200, StatCombatScaling.negativeEffectDurationTicks(2000, 100, true));
        assertEquals(-1, StatCombatScaling.negativeEffectDurationTicks(-1, 100, true));
        assertEquals(0, StatCombatScaling.negativeEffectDurationTicks(0, 100, true));
    }
}
