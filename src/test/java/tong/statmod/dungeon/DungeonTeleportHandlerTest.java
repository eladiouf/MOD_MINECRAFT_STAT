package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DungeonTeleportHandlerTest {

    @Test
    void floor1SpawnsAtOrigin() {
        BlockPos pos = DungeonTeleportHandler.floorSpawnPos(1);
        assertEquals(0, pos.getX());
        assertEquals(100, pos.getY());
        assertEquals(0, pos.getZ());
    }

    @Test
    void floor2IsOneColumnRight() {
        BlockPos pos = DungeonTeleportHandler.floorSpawnPos(2);
        assertEquals(DungeonTeleportHandler.FLOOR_SPACING, pos.getX());
        assertEquals(100, pos.getY());
        assertEquals(0, pos.getZ());
    }

    @Test
    void floor11StartsNewRow() {
        BlockPos pos = DungeonTeleportHandler.floorSpawnPos(11);
        assertEquals(0, pos.getX());
        assertEquals(100, pos.getY());
        assertEquals(DungeonTeleportHandler.FLOOR_SPACING, pos.getZ());
    }

    @Test
    void floorSpawnPosYIsConstant() {
        for (int f = 1; f <= 100; f++) {
            assertEquals(100, DungeonTeleportHandler.floorSpawnPos(f).getY());
        }
    }

    @Test
    void floorAtPosSnapsToNearestFloor() {
        int s = DungeonTeleportHandler.FLOOR_SPACING;
        assertEquals(1, DungeonTeleportHandler.floorAtPos(0, 0));
        assertEquals(1, DungeonTeleportHandler.floorAtPos(s/2 - 1, s/2 - 1));
        assertEquals(2, DungeonTeleportHandler.floorAtPos(s, 0));
        assertEquals(11, DungeonTeleportHandler.floorAtPos(0, s));
        assertEquals(12, DungeonTeleportHandler.floorAtPos(s, s));
    }

    @Test
    void spacingIsConsistent() {
        int s = DungeonTeleportHandler.FLOOR_SPACING;
        BlockPos f1 = DungeonTeleportHandler.floorSpawnPos(1);
        BlockPos f2 = DungeonTeleportHandler.floorSpawnPos(2);
        assertEquals(s, f2.getX() - f1.getX());
        assertEquals(0, f2.getZ() - f1.getZ());
    }
}
