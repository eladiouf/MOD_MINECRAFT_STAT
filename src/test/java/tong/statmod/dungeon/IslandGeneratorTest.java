package tong.statmod.dungeon;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase ε — tests unit sur la classification d'étage (combat / treasure / boss) et sur les
 * dimensions des bounding boxes. Le rendering réel demande un {@code ServerLevel} et est
 * couvert par le test manuel MVP.
 */
public class IslandGeneratorTest {

    private static final int COMBAT_R = 12;
    private static final int TREASURE_R = 15;
    private static final int BOSS_R = 20;

    @Test
    void floor1BoundingBoxIsCombatSized() {
        BoundingBox box = IslandGenerator.floorBoundingBox(1);
        assertNotNull(box);
        assertEquals(2 * COMBAT_R + 1, box.getXSpan());
        assertEquals(2 * COMBAT_R + 1, box.getZSpan());
    }

    @Test
    void floor5IsTreasureSized() {
        BoundingBox box = IslandGenerator.floorBoundingBox(5);
        assertEquals(2 * TREASURE_R + 1, box.getXSpan());
    }

    @Test
    void floor10IsBossSized() {
        BoundingBox box = IslandGenerator.floorBoundingBox(10);
        assertEquals(2 * BOSS_R + 1, box.getXSpan());
    }

    @Test
    void floor7IsCombatSized() {
        BoundingBox box = IslandGenerator.floorBoundingBox(7);
        assertEquals(2 * COMBAT_R + 1, box.getXSpan());
    }

    @Test
    void floor20IsBossNotTreasure() {
        BoundingBox box = IslandGenerator.floorBoundingBox(20);
        assertEquals(2 * BOSS_R + 1, box.getXSpan());
    }

    @Test
    void allBoundingBoxesCenteredOnTheirFloorSpawn() {
        for (int f = 1; f <= 12; f++) {
            BoundingBox box = IslandGenerator.floorBoundingBox(f);
            int cx = (box.minX() + box.maxX()) / 2;
            int cz = (box.minZ() + box.maxZ()) / 2;
            assertEquals(DungeonTeleportHandler.floorSpawnPos(f).getX(), cx,
                    "floor " + f + " X centered on spawn X");
            assertEquals(DungeonTeleportHandler.floorSpawnPos(f).getZ(), cz,
                    "floor " + f + " Z centered on spawn Z");
        }
    }

    @Test
    void floorSpawnPosIsWithinWorldBounds() {
        int spacing = DungeonTeleportHandler.FLOOR_SPACING;
        int cols = DungeonTeleportHandler.GRID_COLS;
        for (int f = 1; f <= 1000; f++) {
            int idx = f - 1;
            int expectedX = (idx % cols) * spacing;
            int expectedZ = (idx / cols) * spacing;
            assertEquals(expectedX, DungeonTeleportHandler.floorSpawnPos(f).getX());
            assertEquals(expectedZ, DungeonTeleportHandler.floorSpawnPos(f).getZ());
            assertEquals(100, DungeonTeleportHandler.floorSpawnPos(f).getY());
        }
    }

    @Test
    void boundingBoxIncludesUndersideCone() {
        // Redesign 2026-07-03 : la box descend sous le spawn de maxDepth + 1 et monte de 6
        // (mur + décor + piliers de dais).
        for (int f : new int[]{1, 5, 10}) {
            BoundingBox box = IslandGenerator.floorBoundingBox(f);
            int spawnY = DungeonTeleportHandler.floorSpawnPos(f).getY();
            int radius = IslandGenerator.radiusFor(f);
            int maxDepth = new IslandShaper(IslandShaper.seedFor(f), radius).maxDepth();
            assertEquals(spawnY - maxDepth - 1, box.minY(), "minY floor " + f);
            assertEquals(spawnY + 6, box.maxY(), "maxY floor " + f);
        }
    }

    @Test
    void radiusForMatchesTemplates() {
        assertEquals(12, IslandGenerator.radiusFor(1));   // combat
        assertEquals(15, IslandGenerator.radiusFor(5));   // treasure
        assertEquals(20, IslandGenerator.radiusFor(10));  // boss
        assertEquals(20, IslandGenerator.radiusFor(20));  // boss prioritaire sur treasure
        assertEquals(12, IslandGenerator.radiusFor(7));   // combat
    }
}
