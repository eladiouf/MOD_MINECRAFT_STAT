package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase β — vérifie le calcul déterministe des positions de spawn par étage
 * dans la grille XZ horizontale.
 */
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
        assertEquals(80, pos.getX());
        assertEquals(100, pos.getY());
        assertEquals(0, pos.getZ());
    }

    @Test
    void floor11StartsNewRow() {
        BlockPos pos = DungeonTeleportHandler.floorSpawnPos(11);
        assertEquals(0, pos.getX());
        assertEquals(100, pos.getY());
        assertEquals(80, pos.getZ());
    }

    @Test
    void floorSpawnPosYIsConstant() {
        for (int f = 1; f <= 100; f++) {
            assertEquals(100, DungeonTeleportHandler.floorSpawnPos(f).getY(),
                    "Y must be 100 for floor " + f);
        }
    }

    @Test
    void floorAtPosSnapsToNearestFloor() {
        assertEquals(1, DungeonTeleportHandler.floorAtPos(0, 0));
        assertEquals(1, DungeonTeleportHandler.floorAtPos(30, 30));
        assertEquals(2, DungeonTeleportHandler.floorAtPos(80, 0));
        assertEquals(11, DungeonTeleportHandler.floorAtPos(0, 80));
        assertEquals(12, DungeonTeleportHandler.floorAtPos(80, 80));
    }

    @Test
    void spacingIsConsistent() {
        BlockPos f1 = DungeonTeleportHandler.floorSpawnPos(1);
        BlockPos f2 = DungeonTeleportHandler.floorSpawnPos(2);
        assertEquals(80, f2.getX() - f1.getX());
        assertEquals(0, f2.getZ() - f1.getZ());
    }
}
