package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tong.statmod.progression.xp.CombatEligibility.CombatDamageProfile;

class CombatEligibilityTest {
    @Test
    void rejectsEnvironmentalMagicAndSelfDamageProfiles() {
        assertFalse(CombatEligibility.isPhysical(profile(true, false, false, false,
                false, false, false, true, false, false)));
        assertFalse(CombatEligibility.isPhysical(profile(false, true, false, false,
                false, false, false, true, false, false)));
        assertFalse(CombatEligibility.isPhysical(profile(false, false, true, false,
                false, false, false, true, false, false)));
        assertFalse(CombatEligibility.isPhysical(profile(false, false, false, true,
                false, false, false, true, false, false)));
        assertFalse(CombatEligibility.isPhysical(profile(false, false, false, false,
                false, false, true, true, false, false)));
        assertFalse(CombatEligibility.isPhysical(profile(false, false, false, false,
                false, false, false, false, false, false)));
    }

    @Test
    void acceptsMeleeProjectileOpponentAndOwnedExplosionProfiles() {
        assertTrue(CombatEligibility.isPhysical(profile(false, false, false, false,
                false, false, false, true, false, false)));
        assertTrue(CombatEligibility.isPhysical(profile(false, false, false, false,
                false, false, false, true, true, false)));
        assertTrue(CombatEligibility.isPhysical(profile(false, false, false, false,
                false, false, false, true, false, false)));
        assertTrue(CombatEligibility.isPhysical(profile(false, false, false, false,
                false, false, false, true, false, true)));
    }

    private static CombatDamageProfile profile(
            boolean fire, boolean bypassesArmor, boolean drowning, boolean fall,
            boolean freezing, boolean lightning, boolean selfInflicted,
            boolean responsibleEntity, boolean projectile, boolean explosion) {
        return new CombatDamageProfile(fire, bypassesArmor, drowning, fall, freezing,
                lightning, selfInflicted, responsibleEntity, projectile, explosion);
    }
}
