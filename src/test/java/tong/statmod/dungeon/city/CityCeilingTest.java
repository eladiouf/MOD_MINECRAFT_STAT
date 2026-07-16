package tong.statmod.dungeon.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CityCeilingTest {

    @Test
    void boundedBetweenFloorAndPeak() {
        for (int x = -CityPlan.RADIUS; x <= CityPlan.RADIUS; x += 7)
            for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z += 7) {
                int h = CityCeiling.heightAt(x, z);
                assertTrue(h >= 148 && h <= CityPlan.CEILING_Y, "plafond hors bornes: " + h);
                assertTrue(h > CityPlan.GROUND_Y + 10, "plafond trop bas au sol: " + h);
            }
    }

    @Test
    void deterministic() {
        assertEquals(CityCeiling.heightAt(12, -488), CityCeiling.heightAt(12, -488));
    }

    @Test
    void higherAtCenterThanRim() {
        int center = CityCeiling.heightAt(CityPlan.CENTER_X, CityPlan.CENTER_Z);
        int rim = CityCeiling.heightAt(CityPlan.CENTER_X + CityPlan.WALL_INNER - 4, CityPlan.CENTER_Z);
        assertTrue(center > rim, "le centre doit être plus haut que le pourtour");
    }
}
