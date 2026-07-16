package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DungeonFloorAdmissionTest {
    @Test
    void publicCityAlwaysAllowsEntry() {
        assertTrue(DungeonFloorAdmission.canEnter(0, "red:a", List.of("blue:b"),
                (a, b) -> false, true));
    }

    @Test
    void emptyChallengeFloorAllowsEntryWhenManagerReady() {
        assertTrue(DungeonFloorAdmission.canEnter(7, "red:a", List.of(),
                (a, b) -> false, true));
    }

    @Test
    void teammatesMayJoinOccupiedChallengeFloor() {
        assertTrue(DungeonFloorAdmission.canEnter(7, "red:b", List.of("red:a", "red:c"),
                (a, b) -> a.split(":")[0].equals(b.split(":")[0]), true));
    }

    @Test
    void anyRivalOccupantRejectsEntry() {
        assertFalse(DungeonFloorAdmission.canEnter(7, "red:b", List.of("red:a", "blue:a"),
                (a, b) -> a.split(":")[0].equals(b.split(":")[0]), true));
    }

    @Test
    void unavailableRequiredManagerRejectsChallengeEntry() {
        assertFalse(DungeonFloorAdmission.canEnter(7, "red:a", List.of(),
                (a, b) -> true, false));
    }
}
