package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DungeonAlertPolicyTest {
    @Test void sightStartsCombat() {
        assertEquals(DungeonAlertState.COMBAT,
                DungeonAlertPolicy.next(DungeonAlertState.IDLE, true, false, false, false));
    }

    @Test void squadAlertRaisesAlertedState() {
        assertEquals(DungeonAlertState.ALERTED,
                DungeonAlertPolicy.next(DungeonAlertState.IDLE, false, true, false, false));
    }

    @Test void recentMemoryCausesSuspicionAndStaleMemoryReturnsIdle() {
        assertEquals(DungeonAlertState.SUSPICIOUS,
                DungeonAlertPolicy.next(DungeonAlertState.COMBAT, false, false, true, false));
        assertEquals(DungeonAlertState.IDLE,
                DungeonAlertPolicy.next(DungeonAlertState.SUSPICIOUS, false, false, false, false));
    }

    @Test void lowHealthHasRetreatPriority() {
        assertEquals(DungeonAlertState.RETREATING,
                DungeonAlertPolicy.next(DungeonAlertState.COMBAT, true, true, true, true));
    }

    @Test void alertsExpireByTimeOrDistance() {
        assertTrue(DungeonAlertPolicy.canShare(100, 200, 48.0 * 48.0));
        assertFalse(DungeonAlertPolicy.canShare(100, 201, 1.0));
        assertFalse(DungeonAlertPolicy.canShare(100, 150, 48.0 * 48.0 + 0.01));
    }
}
