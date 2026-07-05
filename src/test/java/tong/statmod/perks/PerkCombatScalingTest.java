package tong.statmod.perks;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerkCombatScalingTest {
    private static final double EPSILON = 1.0e-6d;

    @Test
    void coreWeaponDamagePerksOnlyApplyToTheirAdvertisedWeaponStat() {
        assertEquals(1.05f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.BRUTE_FORCE, true, true, true), EPSILON);
        assertEquals(1.05f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.BLADE_TECHNIQUE, true, true, true), EPSILON);
        assertEquals(1.05f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.PRECISION, true, true, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.RAPIDITE, true, true, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.ARCANE_POWER, true, true, true), EPSILON);
    }

    @Test
    void missingMatchingCorePerkDoesNotBoostDamage() {
        assertEquals(1.0f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.BRUTE_FORCE, false, true, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.BLADE_TECHNIQUE, true, false, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.coreWeaponDamageMultiplier(
                StatType.PRECISION, true, true, false), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.coreWeaponDamageMultiplier(
                null, true, true, true), EPSILON);
    }

    @Test
    void weaponFamilyPerksOnlyApplyToMatchingWeaponStats() {
        assertEquals(true, PerkCombatScaling.canUseWeaponFamilyPerk(StatType.BRUTE_FORCE, Perk.BRUTE_ACTIVE));
        assertEquals(false, PerkCombatScaling.canUseWeaponFamilyPerk(StatType.BLADE_TECHNIQUE, Perk.BRUTE_ACTIVE));
        assertEquals(false, PerkCombatScaling.canUseWeaponFamilyPerk(StatType.PRECISION, Perk.BLADE_ACTIVE));
        assertEquals(false, PerkCombatScaling.canUseWeaponFamilyPerk(StatType.BRUTE_FORCE, Perk.RAPID_ACTIVE));
        assertEquals(true, PerkCombatScaling.canUseWeaponFamilyPerk(StatType.PRECISION, Perk.PRECI_MASTERY));
    }

    @Test
    void nonWeaponFamilyPerksStayGlobalWhenUsedByCombatEvents() {
        assertEquals(true, PerkCombatScaling.canUseWeaponFamilyPerk(StatType.BRUTE_FORCE, Perk.AGIL_ACTIVE));
        assertEquals(true, PerkCombatScaling.canUseWeaponFamilyPerk(StatType.BLADE_TECHNIQUE, Perk.TRACK_SYNERGY));
        assertEquals(true, PerkCombatScaling.canUseWeaponFamilyPerk(null, Perk.INTIM_SITUATIONAL));
    }

    @Test
    void bladePostKillMultiplierOnlyAppliesDuringWindowWithBladeWeapons() {
        assertEquals(1.5f, PerkCombatScaling.bladePostKillDamageMultiplier(
                StatType.BLADE_TECHNIQUE, true, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.bladePostKillDamageMultiplier(
                StatType.BLADE_TECHNIQUE, true, false), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.bladePostKillDamageMultiplier(
                StatType.BRUTE_FORCE, true, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.bladePostKillDamageMultiplier(
                StatType.BLADE_TECHNIQUE, false, true), EPSILON);
    }

    @Test
    void bruteTranscendenceDamageOnlyAppliesToHeavyWeapons() {
        assertEquals(1.5f, PerkCombatScaling.bruteTranscendenceDamageMultiplier(
                StatType.BRUTE_FORCE, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.bruteTranscendenceDamageMultiplier(
                StatType.BLADE_TECHNIQUE, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.bruteTranscendenceDamageMultiplier(
                StatType.BRUTE_FORCE, false), EPSILON);
    }

    @Test
    void precisionProjectilePerksOnlyApplyToMarkedRangedHits() {
        assertEquals(1.2f, PerkCombatScaling.precisionMarkedProjectileDamageMultiplier(
                StatType.PRECISION, true, true, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.precisionMarkedProjectileDamageMultiplier(
                StatType.PRECISION, true, false, true), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.precisionMarkedProjectileDamageMultiplier(
                StatType.PRECISION, true, true, false), EPSILON);
        assertEquals(1.0f, PerkCombatScaling.precisionMarkedProjectileDamageMultiplier(
                StatType.BLADE_TECHNIQUE, true, true, true), EPSILON);
    }

    @Test
    void precisionPiercingOnlyAppliesToRangedProjectiles() {
        assertEquals(true, PerkCombatScaling.canPierceSecondaryTarget(
                StatType.PRECISION, true, true));
        assertEquals(false, PerkCombatScaling.canPierceSecondaryTarget(
                StatType.PRECISION, false, true));
        assertEquals(false, PerkCombatScaling.canPierceSecondaryTarget(
                StatType.PRECISION, true, false));
        assertEquals(false, PerkCombatScaling.canPierceSecondaryTarget(
                StatType.BRUTE_FORCE, true, true));
    }
}
