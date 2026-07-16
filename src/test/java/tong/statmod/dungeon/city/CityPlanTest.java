package tong.statmod.dungeon.city;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CityPlanTest {

    @Test
    void allSitesFitInsideWall() {
        for (CityPlan.CitySite s : CityPlan.sites()) {
            double d = Math.hypot(s.center().getX() - CityPlan.CENTER_X,
                    s.center().getZ() - CityPlan.CENTER_Z);
            assertTrue(d + s.radius() <= CityPlan.WALL_INNER,
                    s.id() + " dépasse le rempart (reach=" + (d + s.radius()) + ")");
        }
    }

    @Test
    void noTwoSitesOverlap() {
        List<CityPlan.CitySite> sites = CityPlan.sites();
        for (int i = 0; i < sites.size(); i++) {
            for (int j = i + 1; j < sites.size(); j++) {
                CityPlan.CitySite a = sites.get(i), b = sites.get(j);
                if (a.radius() == 0 || b.radius() == 0) continue; // gate = repère ponctuel
                double d = Math.hypot(a.center().getX() - b.center().getX(),
                        a.center().getZ() - b.center().getZ());
                assertTrue(d >= a.radius() + b.radius(),
                        a.id() + " chevauche " + b.id() + " (d=" + d + ")");
            }
        }
    }

    @Test
    void cityIsCompact() {
        // Refonte visuelle : cité resserrée pour tuer l'immense vide place↔rempart.
        assertTrue(CityPlan.RADIUS <= 200, "cité pas resserrée (RADIUS=" + CityPlan.RADIUS + ")");
        assertTrue(CityPlan.WALL_INNER < CityPlan.RADIUS, "rempart incohérent");
    }

    @Test
    void spawnInsidePlazaGateOnWall() {
        BlockPos spawn = CityPlan.playerSpawn();
        assertTrue(CityPlan.inPlaza(spawn.getX(), spawn.getZ()), "spawn hors place");
        BlockPos gate = CityPlan.gateCenter();
        double dg = Math.hypot(gate.getX() - CityPlan.CENTER_X, gate.getZ() - CityPlan.CENTER_Z);
        assertTrue(dg >= CityPlan.WALL_INNER - 14 && dg <= CityPlan.RADIUS, "porte pas sur le rempart sud");
    }
}
