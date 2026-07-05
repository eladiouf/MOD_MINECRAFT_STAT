package tong.statmod.perks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkMobilityScalingTest {
    @Test
    void rapidMasterySlowsNearbyMobsOnlyWhenADodgeActuallyTriggers() {
        assertEquals(40, PerkMobilityScaling.rapidDodgeSlowdownDurationTicks(true, true));
        assertEquals(0, PerkMobilityScaling.rapidDodgeSlowdownDurationTicks(false, true));
        assertEquals(0, PerkMobilityScaling.rapidDodgeSlowdownDurationTicks(true, false));
    }

    @Test
    void rapidTranscendenceFreezesNearbyMobsOnlyOnPerfectDodge() {
        assertEquals(40, PerkMobilityScaling.rapidPerfectDodgeStopDurationTicks(true, true));
        assertEquals(0, PerkMobilityScaling.rapidPerfectDodgeStopDurationTicks(false, true));
        assertEquals(0, PerkMobilityScaling.rapidPerfectDodgeStopDurationTicks(true, false));
    }

    @Test
    void agilityTranscendenceDodgesOnlyDuringThePostDamageWindow() {
        assertTrue(PerkMobilityScaling.agilityUntouchableDodgesIncomingHit(true, true));
        assertFalse(PerkMobilityScaling.agilityUntouchableDodgesIncomingHit(false, true));
        assertFalse(PerkMobilityScaling.agilityUntouchableDodgesIncomingHit(true, false));
    }

    @Test
    void agilityTranscendenceStartsWindowOnlyAfterRealDamage() {
        assertTrue(PerkMobilityScaling.agilityUntouchableStartsWindow(true, 3.0f));
        assertFalse(PerkMobilityScaling.agilityUntouchableStartsWindow(false, 3.0f));
        assertFalse(PerkMobilityScaling.agilityUntouchableStartsWindow(true, 0.0f));
    }
}
