package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomEncounterProgressTest {

    @Test
    void completesOnlyAfterEveryRequiredRoomIsCleared() {
        RoomEncounterProgress progress = new RoomEncounterProgress(Set.of(1, 2, 3));

        progress.activate(1);
        assertFalse(progress.clearActiveRoom());
        progress.activate(3);
        assertFalse(progress.clearActiveRoom());
        progress.activate(2);
        assertTrue(progress.clearActiveRoom());
    }

    @Test
    void cannotActivateASecondEncounterWhileOneIsRunning() {
        RoomEncounterProgress progress = new RoomEncounterProgress(Set.of(1, 2));

        assertTrue(progress.activate(1));
        assertFalse(progress.activate(2));
        assertTrue(progress.isActive(1));
    }

    @Test
    void ignoresRoomsThatAreNotRequiredOrAlreadyCleared() {
        RoomEncounterProgress progress = new RoomEncounterProgress(Set.of(2));

        assertFalse(progress.activate(1));
        assertTrue(progress.activate(2));
        assertTrue(progress.clearActiveRoom());
        assertFalse(progress.activate(2));
    }
}
