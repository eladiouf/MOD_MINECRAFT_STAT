package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import tong.statmod.dungeon.DungeonTeleportHandler;

import java.util.HashSet;
import java.util.List;

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

    @Test
    void finalLandmarksAreUniqueAndInsideCity() {
        List<BlockPos> landmarks = List.of(
                CityPlan.center(), CityPlan.guild(), CityPlan.humanQuarter(), CityPlan.elvenQuarter(),
                CityPlan.dwarvenQuarter(), CityPlan.beastQuarter(), CityPlan.market(),
                CityPlan.artisanDistrict(), CityPlan.arena(), CityPlan.trainingGround(),
                CityPlan.sanctuary(), CityPlan.hangingGardens(), CityPlan.hallOfHeroes(),
                CityPlan.portalCourt(), CityPlan.gateCenter());

        assertEquals(15, new HashSet<>(landmarks).size());
        for (BlockPos landmark : landmarks) {
            assertTrue(CityPlan.inCity(landmark.getX(), landmark.getZ()), landmark + " outside city");
        }
        for (BlockPos landmark : landmarks.subList(1, landmarks.size())) {
            assertFalse(CityPlan.inPlaza(landmark.getX(), landmark.getZ()), landmark + " overlaps plaza");
        }
    }

    @Test
    void ringRoadsConnectDistrictsOutsidePlaza() {
        assertTrue(CityPlan.onRingRoad(CityPlan.CENTER_X + CityPlan.INNER_RING_RADIUS, CityPlan.CENTER_Z));
        assertTrue(CityPlan.onRingRoad(CityPlan.CENTER_X, CityPlan.CENTER_Z + CityPlan.OUTER_RING_RADIUS));
        assertFalse(CityPlan.onRingRoad(CityPlan.CENTER_X + 20, CityPlan.CENTER_Z));
    }

    @Test
    void citySitesHaveBreathingRoomAndStayInsideWalls() {
        List<CityPlan.CitySite> sites = CityPlan.sites();
        for (int i = 0; i < sites.size(); i++) {
            CityPlan.CitySite a = sites.get(i);
            double centerDistance = Math.hypot(a.center().getX() - CityPlan.CENTER_X,
                    a.center().getZ() - CityPlan.CENTER_Z);
            assertTrue(centerDistance + a.radius() < CityPlan.WALL_INNER,
                    a.id() + " crosses city wall");
            for (int j = i + 1; j < sites.size(); j++) {
                CityPlan.CitySite b = sites.get(j);
                double distance = Math.hypot(a.center().getX() - b.center().getX(),
                        a.center().getZ() - b.center().getZ());
                assertTrue(distance >= a.radius() + b.radius() + 8,
                        a.id() + " overlaps " + b.id());
            }
        }
    }

    @Test
    void everyDistrictHasAConnectorTowardThePlaza() {
        for (CityPlan.CitySite site : CityPlan.sites()) {
            if (site.id().equals("plaza") || site.id().equals("gate")) continue;
            int midX = (site.center().getX() + CityPlan.CENTER_X) / 2;
            int midZ = (site.center().getZ() + CityPlan.CENTER_Z) / 2;
            assertTrue(CityPlan.onDistrictConnector(midX, midZ), site.id() + " has no connector");
        }
    }

    @Test
    void pvpArenaIsOnlyTheInnerCombatCircle() {
        BlockPos arena = CityPlan.arena();
        assertTrue(CityPlan.inArenaCombat(arena.getX(), arena.getZ()));
        assertTrue(CityPlan.inArenaCombat(arena.getX() + 25, arena.getZ()));
        assertFalse(CityPlan.inArenaCombat(arena.getX() + 27, arena.getZ()));
        assertFalse(CityPlan.inArenaCombat(CityPlan.center().getX(), CityPlan.center().getZ()));
    }
}
