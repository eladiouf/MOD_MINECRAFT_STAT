package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DungeonDirectorPolicyTest {
    @Test
    void ignoresPlayersWhoCannotParticipate() {
        var players = List.of(
                new DungeonDirectorPolicy.PlayerSnapshot(4, true, false, true),
                new DungeonDirectorPolicy.PlayerSnapshot(5, false, true, true),
                new DungeonDirectorPolicy.PlayerSnapshot(6, false, false, false));
        assertEquals(Set.of(), DungeonDirectorPolicy.occupiedFloors(players));
    }

    @Test
    void scansEachOccupiedFloorOnce() {
        var players = List.of(
                new DungeonDirectorPolicy.PlayerSnapshot(12, false, false, true),
                new DungeonDirectorPolicy.PlayerSnapshot(12, false, false, true),
                new DungeonDirectorPolicy.PlayerSnapshot(19, false, false, true));
        assertEquals(Set.of(12, 19), DungeonDirectorPolicy.occupiedFloors(players));
    }
}
