package tong.statmod.dungeon;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IslandGeneratorTest {

    // Référence les constantes de production pour ne jamais dériver si l'île est redimensionnée.
    static final int R = IslandGenerator.R;
    // La box de regen dépasse le rayon d'île pour couvrir la cage barrière (R+2) — audit 2026-07-09.
    static final int SPAN = 2 * (R + IslandGenerator.CLEAR_MARGIN) + 1;

    @Test void floor1BoundingBoxIsUniform() { BoundingBox b = IslandGenerator.floorBoundingBox(1); assertNotNull(b); assertEquals(SPAN, b.getXSpan()); }
    @Test void floor5SameSize() { assertEquals(SPAN, IslandGenerator.floorBoundingBox(5).getXSpan()); }
    @Test void floor10SameSize() { assertEquals(SPAN, IslandGenerator.floorBoundingBox(10).getXSpan()); }
    @Test void floor7SameSize() { assertEquals(SPAN, IslandGenerator.floorBoundingBox(7).getXSpan()); }
    @Test void floor20SameSize() { assertEquals(SPAN, IslandGenerator.floorBoundingBox(20).getXSpan()); }

    @Test void boxCoversBarrierCageAndUndersideCone() {
        BoundingBox b = IslandGenerator.floorBoundingBox(1);
        var sp = DungeonTeleportHandler.floorSpawnPos(1);
        assertTrue(b.minX() <= sp.getX() - (R + 2) && b.maxX() >= sp.getX() + (R + 2),
                "la box doit englober les murs de la cage barrière (±R+2)");
        assertTrue(b.minY() <= sp.getY() - 48, "la box doit englober le plancher de la cage (−48)");
        assertTrue(b.maxY() >= sp.getY() + 50, "la box doit englober le plafond de la cage (+50)");
    }

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
