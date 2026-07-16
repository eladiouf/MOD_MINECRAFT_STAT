package tong.statmod.dungeon.party;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PartyTargetingTest {

    @Test
    void focusPicksLowestHpPlayer() {
        double[] hp = {1.0, 0.3, 0.9};
        double[][] pos = {{0, 0}, {20, 0}, {5, 5}};
        assertEquals(1, PartyTargeting.focusIndex(hp, pos, 0, 0));
    }

    @Test
    void focusTieBreaksByProximityToParty() {
        double[] hp = {0.5, 0.5};
        double[][] pos = {{30, 0}, {3, 0}}; // même PV → le plus proche du groupe (0,0)
        assertEquals(1, PartyTargeting.focusIndex(hp, pos, 0, 0));
    }

    @Test
    void isolatedPicksPlayerFarthestFromAllies() {
        double[][] pos = {{0, 0}, {2, 0}, {50, 50}}; // le 3e est isolé
        assertEquals(2, PartyTargeting.isolatedIndex(pos));
    }

    @Test
    void isolatedWithSinglePlayerIsThatPlayer() {
        assertEquals(0, PartyTargeting.isolatedIndex(new double[][]{{7, 7}}));
    }

    @Test
    void nearestToBacklinePicksClosestThreat() {
        double[][] pos = {{100, 0}, {12, 3}, {40, 40}};
        assertEquals(1, PartyTargeting.nearestToPoint(pos, 10, 0));
    }

    @Test
    void clusterCountsPlayersInRadius() {
        double[][] pos = {{0, 0}, {2, 1}, {3, 0}, {40, 40}};
        assertEquals(3, PartyTargeting.clusterSize(pos, 1, 0, 5));
    }

    @Test
    void emptyPlayersYieldsNoTarget() {
        assertEquals(-1, PartyTargeting.focusIndex(new double[0], new double[0][], 0, 0));
        assertEquals(-1, PartyTargeting.isolatedIndex(new double[0][]));
    }
}
