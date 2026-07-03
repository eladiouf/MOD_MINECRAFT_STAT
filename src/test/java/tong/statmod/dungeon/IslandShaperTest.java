package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Island Redesign Phase A — géométrie pure, aucune dépendance Bootstrap.
 */
public class IslandShaperTest {

    private static final int RADIUS = 12;

    @Test
    void sameSeedIsDeterministic() {
        IslandShaper a = new IslandShaper(42L, RADIUS);
        IslandShaper b = new IslandShaper(42L, RADIUS);
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                assertEquals(a.isInside(dx, dz), b.isInside(dx, dz),
                        "isInside diverge en (" + dx + "," + dz + ")");
                assertEquals(a.depthAt(dx, dz), b.depthAt(dx, dz),
                        "depthAt diverge en (" + dx + "," + dz + ")");
            }
        }
    }

    @Test
    void differentSeedsGiveDifferentSilhouettes() {
        IslandShaper a = new IslandShaper(IslandShaper.seedFor(1), RADIUS);
        IslandShaper b = new IslandShaper(IslandShaper.seedFor(2), RADIUS);
        boolean anyDiff = false;
        for (int dx = -RADIUS; dx <= RADIUS && !anyDiff; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS && !anyDiff; dz++) {
                if (a.isInside(dx, dz) != b.isInside(dx, dz)) anyDiff = true;
            }
        }
        assertTrue(anyDiff, "Deux seeds différents doivent produire des silhouettes différentes");
    }

    @Test
    void centerIsAlwaysInside() {
        for (int floor = 1; floor <= 100; floor++) {
            IslandShaper s = new IslandShaper(IslandShaper.seedFor(floor), RADIUS);
            assertTrue(s.isInside(0, 0), "centre outside pour floor " + floor);
        }
    }

    @Test
    void nothingOutsideRadiusBound() {
        IslandShaper s = new IslandShaper(IslandShaper.seedFor(7), RADIUS);
        for (int dx = -RADIUS - 3; dx <= RADIUS + 3; dx++) {
            for (int dz = -RADIUS - 3; dz <= RADIUS + 3; dz++) {
                if (Math.sqrt((double) dx * dx + dz * dz) > RADIUS) {
                    assertFalse(s.isInside(dx, dz),
                            "cellule inside au-delà de R en (" + dx + "," + dz + ")");
                }
            }
        }
    }

    @Test
    void radiusAtStaysWithinModulationBounds() {
        IslandShaper s = new IslandShaper(IslandShaper.seedFor(13), RADIUS);
        for (int i = 0; i < 360; i++) {
            double theta = Math.toRadians(i);
            double r = s.radiusAt(theta);
            assertTrue(r >= RADIUS * IslandShaper.MIN_FACTOR - 1e-9,
                    "radiusAt sous le min à θ=" + i + " : " + r);
            assertTrue(r <= RADIUS + 1e-9, "radiusAt au-dessus de R à θ=" + i + " : " + r);
        }
    }

    @Test
    void boundaryCellsHaveAnOutsideNeighbor() {
        IslandShaper s = new IslandShaper(IslandShaper.seedFor(3), RADIUS);
        int boundaryCount = 0;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (!s.isBoundary(dx, dz)) continue;
                boundaryCount++;
                assertTrue(s.isInside(dx, dz), "boundary doit être inside");
                boolean hasOutsideNeighbor = !s.isInside(dx + 1, dz) || !s.isInside(dx - 1, dz)
                        || !s.isInside(dx, dz + 1) || !s.isInside(dx, dz - 1);
                assertTrue(hasOutsideNeighbor, "boundary sans voisin outside en (" + dx + "," + dz + ")");
            }
        }
        assertTrue(boundaryCount > 20, "un anneau boundary complet est attendu, count=" + boundaryCount);
    }

    @Test
    void depthIsZeroOutsideAndPositiveInside() {
        IslandShaper s = new IslandShaper(IslandShaper.seedFor(5), RADIUS);
        assertEquals(0, s.depthAt(RADIUS + 2, 0));
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int d = s.depthAt(dx, dz);
                if (s.isInside(dx, dz)) {
                    assertTrue(d >= 1 && d <= s.maxDepth(),
                            "depth hors bornes en (" + dx + "," + dz + ") : " + d);
                } else {
                    assertEquals(0, d);
                }
            }
        }
    }

    @Test
    void depthIsDeeperAtCenterThanNearEdge() {
        IslandShaper s = new IslandShaper(IslandShaper.seedFor(9), RADIUS);
        int centerDepth = s.depthAt(0, 0);
        // Cellule proche du bord garanti (0.82·R min) : dist R-2 sur l'axe X.
        int edgeDepth = s.isInside(RADIUS - 3, 0) ? s.depthAt(RADIUS - 3, 0) : 1;
        assertTrue(centerDepth > edgeDepth,
                "centre (" + centerDepth + ") doit être plus profond que le bord (" + edgeDepth + ")");
    }

    @Test
    void seedForIsStablePerFloorAndDistinct() {
        assertEquals(IslandShaper.seedFor(10), IslandShaper.seedFor(10));
        assertTrue(IslandShaper.seedFor(1) != IslandShaper.seedFor(2));
        assertTrue(IslandShaper.seedFor(10) != IslandShaper.seedFor(20));
    }

    @Test
    void maxDepthScalesWithRadius() {
        assertEquals(8, new IslandShaper(1L, 12).maxDepth());
        assertEquals(9, new IslandShaper(1L, 15).maxDepth());
        assertEquals(12, new IslandShaper(1L, 20).maxDepth());
    }
}
