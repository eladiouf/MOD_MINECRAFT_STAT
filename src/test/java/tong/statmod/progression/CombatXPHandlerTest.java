package tong.statmod.progression;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatXPHandlerTest {

    // XP formula: Math.max(1, Math.round(target.getMaxHealth() * 1.5f))

    @Test
    void zombieTwentyHpYieldsThirtyXp() {
        int xp = Math.max(1, Math.round(20f * 1.5f));
        assertEquals(30, xp);
    }

    @Test
    void skeletonTwentyHpYieldsThirtyXp() {
        int xp = Math.max(1, Math.round(20f * 1.5f));
        assertEquals(30, xp);
    }

    @Test
    void witherThreeHundredHpYieldsFourFiftyXp() {
        int xp = Math.max(1, Math.round(300f * 1.5f));
        assertEquals(450, xp);
    }

    @Test
    void zeroHpClampedToOneXp() {
        int xp = Math.max(1, Math.round(0f * 1.5f));
        assertEquals(1, xp);
    }

    @Test
    void negativeHpClampedToOneXp() {
        int xp = Math.max(1, Math.round(-10f * 1.5f));
        assertEquals(1, xp);
    }

    // resolveWeaponStat delegates to WeaponResolver

    @Test
    void resolveWeaponStatMapsEmptyToBruteForce() {
        assertEquals(StatType.BRUTE_FORCE, WeaponResolver.statForPath(""));
    }

    @Test
    void resolveWeaponStatMapsNullToBruteForce() {
        assertEquals(StatType.BRUTE_FORCE, WeaponResolver.statForPath(null));
    }

    @Test
    void resolveWeaponStatMapsSwordToBladeTechnique() {
        assertEquals(StatType.BLADE_TECHNIQUE, WeaponResolver.statForPath("iron_sword"));
    }

    @Test
    void resolveWeaponStatMapsAxeToBruteForce() {
        assertEquals(StatType.BRUTE_FORCE, WeaponResolver.statForPath("diamond_axe"));
    }

    @Test
    void resolveWeaponStatMapsBowToPrecision() {
        assertEquals(StatType.PRECISION, WeaponResolver.statForPath("bow"));
    }

    @Test
    void resolveWeaponStatMapsStaffToArcanePower() {
        assertEquals(StatType.ARCANE_POWER, WeaponResolver.statForPath("staff"));
    }

    @Test
    void combatStatForWeaponCategorySwordReturnsBladeTechnique() {
        assertEquals(StatType.BLADE_TECHNIQUE, WeaponResolver.combatStatForWeaponCategory("SWORD"));
    }

    @Test
    void combatStatForWeaponCategoryAxeReturnsBruteForce() {
        assertEquals(StatType.BRUTE_FORCE, WeaponResolver.combatStatForWeaponCategory("AXE"));
    }

    @Test
    void combatStatForWeaponCategoryBowReturnsPrecision() {
        assertEquals(StatType.PRECISION, WeaponResolver.combatStatForWeaponCategory("BOW"));
    }

    @Test
    void combatStatForWeaponCategoryStaffReturnsArcanePower() {
        assertEquals(StatType.ARCANE_POWER, WeaponResolver.combatStatForWeaponCategory("STAFF"));
    }
}
