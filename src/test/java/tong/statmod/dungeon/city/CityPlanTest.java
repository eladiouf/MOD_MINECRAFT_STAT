package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import tong.statmod.dungeon.DungeonTeleportHandler;

import static org.junit.jupiter.api.Assertions.*;

class CityPlanTest {

    /** Toute l'emprise de la cité est dans la zone étage 0 (z < -150) — jamais sur la grille 1+. */
    @Test
    void entireCityIsInFloorZeroZone() {
        for (int x = -CityPlan.RADIUS; x <= CityPlan.RADIUS; x += 25) {
            for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z += 25) {
                if (!CityPlan.inCity(x, z)) continue;
                assertEquals(0, DungeonTeleportHandler.floorAtPos(x, z),
                        "(" + x + "," + z + ") doit être étage 0");
            }
        }
    }

    @Test
    void playerSpawnIsInsidePlaza() {
        BlockPos sp = CityPlan.playerSpawn();
        assertTrue(CityPlan.inPlaza(sp.getX(), sp.getZ()));
        assertEquals(CityPlan.GROUND_Y + 1, sp.getY());
    }

    /** La Porte du Donjon est au sud (z max) de la cité, dans l'emprise. */
    @Test
    void gateIsAtSouthEdge() {
        BlockPos gate = CityPlan.gateCenter();
        assertTrue(CityPlan.inCity(gate.getX(), gate.getZ()));
        assertTrue(gate.getZ() > CityPlan.CENTER_Z + CityPlan.WALL_INNER - 40,
                "la porte perce le rempart sud");
    }

    /** L'ouverture de la porte troue bien le rempart ; ailleurs le rempart est plein. */
    @Test
    void gateOpeningPiercesWallOnlyAtSouth() {
        BlockPos gate = CityPlan.gateCenter();
        assertTrue(CityPlan.inGateOpening(gate.getX(), gate.getZ()));
        assertFalse(CityPlan.inGateOpening(CityPlan.CENTER_X, CityPlan.CENTER_Z - CityPlan.WALL_INNER - 2),
                "pas d'ouverture au nord");
        assertTrue(CityPlan.inWallRing(CityPlan.CENTER_X, CityPlan.CENTER_Z - CityPlan.RADIUS + 4));
    }

    /** L'avenue sud relie la place à la porte (continuité tous les 10 blocs). */
    @Test
    void southAvenueConnectsPlazaToGate() {
        for (int z = CityPlan.CENTER_Z + CityPlan.PLAZA_RADIUS + 1;
             z < CityPlan.CENTER_Z + CityPlan.WALL_INNER - 4; z += 10) {
            assertTrue(CityPlan.onAvenue(CityPlan.CENTER_X, z),
                    "avenue sud interrompue en z=" + z);
        }
    }

    /** Les avenues ne mordent ni sur la place ni sur le rempart. */
    @Test
    void avenuesStayBetweenPlazaAndWall() {
        assertFalse(CityPlan.onAvenue(CityPlan.CENTER_X, CityPlan.CENTER_Z + 10));
        assertFalse(CityPlan.onAvenue(CityPlan.CENTER_X, CityPlan.CENTER_Z + CityPlan.RADIUS - 1));
    }

    /** La cour des portails et le camp des artisans sont dans la cité, hors de la place. */
    @Test
    void annexesAreInsideCityOutsidePlaza() {
        for (BlockPos p : new BlockPos[]{CityPlan.portalCourt(), CityPlan.artisanCamp()}) {
            assertTrue(CityPlan.inCity(p.getX(), p.getZ()));
            assertFalse(CityPlan.inPlaza(p.getX(), p.getZ()));
        }
    }

    /** L'ancienne île hub (0,-300) est bien dans l'emprise (elle sera nettoyée/écrasée). */
    @Test
    void legacyHubIsInsideCityFootprint() {
        assertTrue(CityPlan.inCity(0, -300));
    }
}
