package tong.statmod.dungeon;

import java.util.HashSet;
import java.util.Set;

/** Pure runtime state for one floor's sequential room encounters. */
final class RoomEncounterProgress {
    private final Set<Integer> requiredRooms;
    private final Set<Integer> clearedRooms = new HashSet<>();
    private int activeRoom = -1;

    RoomEncounterProgress(Set<Integer> requiredRooms) {
        this.requiredRooms = Set.copyOf(requiredRooms);
    }

    boolean activate(int room) {
        if (activeRoom >= 0 || !requiredRooms.contains(room) || clearedRooms.contains(room)) return false;
        activeRoom = room;
        return true;
    }

    boolean clearActiveRoom() {
        if (activeRoom >= 0) clearedRooms.add(activeRoom);
        activeRoom = -1;
        return isComplete();
    }

    void resetActiveRoom() {
        activeRoom = -1;
    }

    boolean isActive(int room) {
        return activeRoom == room;
    }

    boolean hasActiveRoom() {
        return activeRoom >= 0;
    }

    boolean isCleared(int room) {
        return clearedRooms.contains(room);
    }

    boolean isComplete() {
        return clearedRooms.containsAll(requiredRooms);
    }
}
