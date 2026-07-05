package tong.statmod.perks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkPerceptionScalingTest {
    @Test
    void trackingSituationalLeavesAScentTrailOnlyFromRealHits() {
        assertEquals(200, PerkPerceptionScaling.scentTrailDurationTicks(true, true));
        assertEquals(0, PerkPerceptionScaling.scentTrailDurationTicks(false, true));
        assertEquals(0, PerkPerceptionScaling.scentTrailDurationTicks(true, false));
    }

    @Test
    void senseSynergyRevealsOnlyHiddenMobs() {
        assertEquals(60, PerkPerceptionScaling.hiddenMobRevealDurationTicks(true, true));
        assertEquals(0, PerkPerceptionScaling.hiddenMobRevealDurationTicks(false, true));
        assertEquals(0, PerkPerceptionScaling.hiddenMobRevealDurationTicks(true, false));
    }

    @Test
    void senseTranscendenceHealthReadoutRequiresAliveTargets() {
        assertTrue(PerkPerceptionScaling.canRevealHealthReadout(true, true));
        assertFalse(PerkPerceptionScaling.canRevealHealthReadout(false, true));
        assertFalse(PerkPerceptionScaling.canRevealHealthReadout(true, false));
    }
}
