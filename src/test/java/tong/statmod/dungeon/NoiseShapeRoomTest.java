package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;
import tong.statmod.dungeon.layout.NoiseShapeRoom;

import static org.junit.jupiter.api.Assertions.*;

public class NoiseShapeRoomTest {

    private static final long FLOOR_SEED = 42L;

    @Test
    void shapeDeterministic() {
        NoiseShapeRoom a = new NoiseShapeRoom(FLOOR_SEED, 0);
        NoiseShapeRoom b = new NoiseShapeRoom(FLOOR_SEED, 0);
        for (int w = 20; w <= 60; w += 5) {
            for (int d = 20; d <= 60; d += 5) {
                assertEquals(a.inside(10, 10, w, d), b.inside(10, 10, w, d));
            }
        }
    }

    @Test
    void centerAlwaysInside() {
        NoiseShapeRoom shape = new NoiseShapeRoom(FLOOR_SEED, 3);
        for (int w = 20; w <= 60; w += 5) {
            for (int d = 20; d <= 60; d += 5) {
                assertTrue(shape.inside(w / 2, d / 2, w, d),
                        "center should be inside w=" + w + " d=" + d);
            }
        }
    }

    @Test
    void cornersNotAlwaysInside() {
        NoiseShapeRoom shape = new NoiseShapeRoom(FLOOR_SEED, 7);
        int w = 40, d = 40;
        boolean anyCornerOut = false;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                int lx = (dx == -1) ? 0 : w;
                int lz = (dz == -1) ? 0 : d;
                if (!shape.inside(lx, lz, w, d)) anyCornerOut = true;
            }
        }
        assertTrue(anyCornerOut, "au moins un coin doit être extérieur pour une forme organique");
    }

    @Test
    void doorwaysRemainOpen() {
        NoiseShapeRoom shape = new NoiseShapeRoom(FLOOR_SEED, 5);
        int w = 40, d = 40;
        assertTrue(shape.inside(w / 2, 0, w, d), "north door");
        assertTrue(shape.inside(w / 2, d, w, d), "south door");
        assertTrue(shape.inside(0, d / 2, w, d), "west door");
        assertTrue(shape.inside(w, d / 2, w, d), "east door");
        assertTrue(shape.inside(w / 2 - 1, 0, w, d));
        assertTrue(shape.inside(w / 2 + 1, 0, w, d));
    }

    @Test
    void outsideBounds() {
        NoiseShapeRoom shape = new NoiseShapeRoom(FLOOR_SEED, 1);
        assertFalse(shape.inside(-1, 5, 40, 40));
        assertFalse(shape.inside(5, -2, 40, 40));
        assertFalse(shape.inside(41, 20, 40, 40));
        assertFalse(shape.inside(20, 41, 40, 40));
    }

    @Test
    void uniqueShapePerRoom() {
        NoiseShapeRoom a = new NoiseShapeRoom(FLOOR_SEED, 0);
        NoiseShapeRoom b = new NoiseShapeRoom(FLOOR_SEED, 1);
        boolean different = false;
        int w = 40, d = 40;
        for (int lx = 0; lx <= w; lx++) {
            for (int lz = 0; lz <= d; lz++) {
                if (a.inside(lx, lz, w, d) != b.inside(lx, lz, w, d)) {
                    different = true;
                    break;
                }
            }
            if (different) break;
        }
        assertTrue(different, "deux salles différentes doivent avoir des formes différentes");
    }

    @Test
    void insideMatchesInsideAbs() {
        NoiseShapeRoom shape = new NoiseShapeRoom(FLOOR_SEED, 2);
        int minX = -50, minZ = -40, maxX = 10, maxZ = 20;
        int w = maxX - minX, d = maxZ - minZ;
        for (int x = minX; x <= maxX; x += 3) {
            for (int z = minZ; z <= maxZ; z += 3) {
                boolean local = shape.inside(x - minX, z - minZ, w, d);
                boolean abs = shape.insideAbs(x, z, minX, minZ, maxX, maxZ);
                assertEquals(local, abs, "inside vs insideAbs mismatch at " + x + "," + z);
            }
        }
    }
}
