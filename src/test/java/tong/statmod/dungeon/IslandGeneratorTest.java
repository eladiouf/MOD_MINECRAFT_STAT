package tong.statmod.dungeon;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IslandGeneratorTest {

    // Référence la constante de production pour ne jamais dériver si l'île est redimensionnée.
    static final int R = IslandGenerator.R;

    @Test void floor1BoundingBoxIsUniform() { BoundingBox b = IslandGenerator.floorBoundingBox(1); assertNotNull(b); assertEquals(2*R+1, b.getXSpan()); }
    @Test void floor5SameSize() { assertEquals(2*R+1, IslandGenerator.floorBoundingBox(5).getXSpan()); }
    @Test void floor10SameSize() { assertEquals(2*R+1, IslandGenerator.floorBoundingBox(10).getXSpan()); }
    @Test void floor7SameSize() { assertEquals(2*R+1, IslandGenerator.floorBoundingBox(7).getXSpan()); }
    @Test void floor20SameSize() { assertEquals(2*R+1, IslandGenerator.floorBoundingBox(20).getXSpan()); }

    @Test void allBoxesCentered() {
        for (int f = 1; f <= 12; f++) {
            BoundingBox b = IslandGenerator.floorBoundingBox(f);
            int cx = (b.minX()+b.maxX())/2, cz = (b.minZ()+b.maxZ())/2;
            assertEquals(DungeonTeleportHandler.floorSpawnPos(f).getX(), cx);
            assertEquals(DungeonTeleportHandler.floorSpawnPos(f).getZ(), cz);
        }
    }

    @Test void radiusAlwaysSame() {
        for (int f = 1; f <= 100; f++) assertEquals(R, IslandGenerator.radiusFor(f));
    }
}
