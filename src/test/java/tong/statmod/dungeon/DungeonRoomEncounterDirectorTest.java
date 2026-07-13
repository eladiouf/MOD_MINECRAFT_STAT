package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonRoomEncounterDirectorTest {

    @Test
    void resolvesEveryRoomFromItsWorldCenter() {
        int floor = 37;
        BlockPos origin = DungeonTeleportHandler.floorSpawnPos(floor);

        for (DungeonLayout.Room room : DungeonLayout.rooms()) {
            BlockPos center = origin.offset(room.centerX(), 0, room.centerZ());
            assertEquals(room.index(), DungeonRoomEncounterDirector.roomAt(floor, center));
        }
    }

    @Test
    void requiresCombatRoomsButExcludesSpawnAndSafeRoom() {
        Set<Integer> required = DungeonRoomEncounterDirector.requiredRoomIndices();

        assertEquals(DungeonLayout.ROOM_COUNT - 2, required.size());
        assertFalse(required.contains(0));
        assertFalse(required.contains(DungeonLayout.ROOM_COUNT / 2));
        assertTrue(required.contains(DungeonLayout.ROOM_COUNT - 1));
    }
}
