package tong.statmod.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityXpScalingTest {
    @Test
    void sprintTrainingRewardsAgilityAndEnduranceOnlyWhenActuallyMoving() {
        assertEquals(1, ActivityXpScaling.agilityXpForMovement(true, true, false, false));
        assertEquals(0, ActivityXpScaling.agilityXpForMovement(false, true, false, false));
        assertEquals(0, ActivityXpScaling.agilityXpForMovement(true, false, false, false));
        assertEquals(0, ActivityXpScaling.agilityXpForMovement(true, true, true, false));
        assertEquals(0, ActivityXpScaling.agilityXpForMovement(true, true, false, true));

        assertEquals(1, ActivityXpScaling.enduranceXpForMovement(true, true, false, false, false));
        assertEquals(1, ActivityXpScaling.enduranceXpForMovement(true, false, true, false, false));
        assertEquals(0, ActivityXpScaling.enduranceXpForMovement(true, false, false, false, false));
        assertEquals(0, ActivityXpScaling.enduranceXpForMovement(true, true, false, true, false));
        assertEquals(0, ActivityXpScaling.enduranceXpForMovement(true, true, false, false, true));
    }

    @Test
    void physicalEnduranceXpScalesWithIncomingDamageWithoutExploding() {
        assertEquals(0, ActivityXpScaling.enduranceXpForPhysicalDamage(0.0f));
        assertEquals(1, ActivityXpScaling.enduranceXpForPhysicalDamage(1.0f));
        assertEquals(1, ActivityXpScaling.enduranceXpForPhysicalDamage(8.0f));
        assertEquals(6, ActivityXpScaling.enduranceXpForPhysicalDamage(100.0f));
    }

    @Test
    void intimidationXpRequiresCloseThreateningKills() {
        assertEquals(1, ActivityXpScaling.intimidationXpForKill(3.5d, true, 20.0f));
        assertEquals(3, ActivityXpScaling.intimidationXpForKill(2.0d, true, 60.0f));
        assertEquals(0, ActivityXpScaling.intimidationXpForKill(4.1d, true, 20.0f));
        assertEquals(0, ActivityXpScaling.intimidationXpForKill(3.5d, false, 20.0f));
        assertEquals(0, ActivityXpScaling.intimidationXpForKill(3.5d, true, 0.0f));
    }
}
