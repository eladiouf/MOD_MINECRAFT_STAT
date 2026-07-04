package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Refonte terrain 2026-07-04 — invariants de la géométrie pure du pourtour (aucun Bootstrap).
 *
 * <p>Verrouille les deux garanties qui protègent le donjon :
 * <ol>
 *   <li>Le relief ne touche jamais l'emprise de la forteresse (intérieur plat).</li>
 *   <li>Le relief est toujours ≥ 0 (montant uniquement → jamais de trou dans le sol).</li>
 * </ol>
 */
public class IslandTerrainShaperTest {

    private static final int RADIUS = 50;

    @Test
    void neverTouchesFortressFootprint() {
        IslandTerrainShaper t = new IslandTerrainShaper(IslandShaper.seedFor(7), 7, RADIUS);
        for (int dx = -DungeonArchitect.HX; dx <= DungeonArchitect.HX; dx++) {
            for (int dz = -DungeonArchitect.HZ; dz <= DungeonArchitect.HZ; dz++) {
                assertEquals(0, t.rimHeightAt(dx, dz),
                        "Relief non nul dans l'emprise forteresse en (" + dx + "," + dz + ")");
            }
        }
    }

    @Test
    void heightNeverNegative() {
        IslandTerrainShaper t = new IslandTerrainShaper(IslandShaper.seedFor(53), 53, RADIUS);
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                assertTrue(t.rimHeightAt(dx, dz) >= 0,
                        "Hauteur négative (trou) en (" + dx + "," + dz + ")");
            }
        }
    }

    @Test
    void heightBoundedByMax() {
        IslandTerrainShaper t = new IslandTerrainShaper(IslandShaper.seedFor(99), 99, RADIUS);
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                assertTrue(t.rimHeightAt(dx, dz) <= 4,
                        "Relief au-dessus du plafond en (" + dx + "," + dz + ")");
            }
        }
    }

    @Test
    void deterministicForSameSeed() {
        IslandTerrainShaper a = new IslandTerrainShaper(1234L, 12, RADIUS);
        IslandTerrainShaper b = new IslandTerrainShaper(1234L, 12, RADIUS);
        for (int dx = -RADIUS; dx <= RADIUS; dx += 3) {
            for (int dz = -RADIUS; dz <= RADIUS; dz += 3) {
                assertEquals(a.rimHeightAt(dx, dz), b.rimHeightAt(dx, dz),
                        "Non déterministe en (" + dx + "," + dz + ")");
            }
        }
    }

    @Test
    void taperRisesFromWall() {
        // Juste au bord du mur (out=1) le relief est atténué ; loin du mur il peut monter plus haut.
        IslandTerrainShaper t = new IslandTerrainShaper(IslandShaper.seedFor(3), 3, RADIUS);
        // Sur une bande le long de +X hors emprise, la hauteur max près du mur ≤ hauteur max au bord.
        int maxNearWall = 0, maxFarWall = 0;
        for (int dz = -DungeonArchitect.HZ; dz <= DungeonArchitect.HZ; dz++) {
            maxNearWall = Math.max(maxNearWall, t.rimHeightAt(DungeonArchitect.HX + 1, dz));
            maxFarWall = Math.max(maxFarWall, t.rimHeightAt(DungeonArchitect.HX + 6, dz));
        }
        assertTrue(maxNearWall <= maxFarWall,
                "Le relief près du mur (" + maxNearWall + ") devrait être ≤ loin du mur (" + maxFarWall + ")");
    }
}
