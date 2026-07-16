package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DungeonPointBalanceTest {
    @Test
    void clearAndBossRewardsFollowReferenceCurve() {
        int[][] values = {
                {1, 118, 830}, {10, 340, 1650}, {25, 670, 2950},
                {50, 1300, 5250}, {100, 2500, 9750}, {150, 3700, 14250}
        };
        for (int[] value : values) {
            assertEquals(value[1], DungeonPointBalance.floorClearReward(value[0]));
            assertEquals(value[2], DungeonPointBalance.bossReward(value[0]));
        }
    }

    @Test
    void mobRewardUsesDepthWithDynamicMinimumAndCap() {
        assertEquals(18, DungeonPointBalance.mobReward(100.0, 1));
        assertEquals(27, DungeonPointBalance.mobReward(100.0, 100));
        assertEquals(3, DungeonPointBalance.mobReward(0.0, 1));
        assertEquals(7, DungeonPointBalance.mobReward(0.0, 100));
        assertEquals(123, DungeonPointBalance.mobReward(100_000.0, 1));
        assertEquals(420, DungeonPointBalance.mobReward(100_000.0, 100));
        assertEquals(600, DungeonPointBalance.mobReward(100_000.0, 1000));
    }

    @Test
    void invalidDifficultyCannotCreateInvalidPoints() {
        assertEquals(3, DungeonPointBalance.mobReward(Double.NaN, 1));
        assertEquals(3, DungeonPointBalance.mobReward(Double.NEGATIVE_INFINITY, 1));
        assertEquals(3, DungeonPointBalance.mobReward(-500.0, 1));
    }

    @Test
    void deathRiskGrowsWithDepthAndNeverExceedsBalance() {
        assertEquals(1010, DungeonPointBalance.deathLoss(10_000, 1));
        assertEquals(1100, DungeonPointBalance.deathLoss(10_000, 10));
        assertEquals(1250, DungeonPointBalance.deathLoss(10_000, 25));
        assertEquals(1500, DungeonPointBalance.deathLoss(10_000, 50));
        assertEquals(2000, DungeonPointBalance.deathLoss(10_000, 100));
        assertEquals(2500, DungeonPointBalance.deathLoss(10_000, 150));
        assertEquals(100, DungeonPointBalance.deathLoss(100, 100));
        assertEquals(0, DungeonPointBalance.deathLoss(0, 100));
        assertEquals(0, DungeonPointBalance.deathLoss(-10, 100));
    }

    @Test
    void assistsStayAtFortyPercentAndClampInvalidInput() {
        assertEquals(40, DungeonPointBalance.assistShare(100));
        assertEquals(1, DungeonPointBalance.assistShare(3));
        assertEquals(0, DungeonPointBalance.assistShare(0));
        assertEquals(0, DungeonPointBalance.assistShare(-50));
    }

    @Test
    void rewardsAndRiskAreMonotonicAcrossSupportedDepths() {
        int previousClear = 0;
        int previousBoss = 0;
        int previousMob = 0;
        int previousLoss = 0;
        for (int floor = 1; floor <= 1000; floor++) {
            int clear = DungeonPointBalance.floorClearReward(floor);
            int boss = DungeonPointBalance.bossReward(floor);
            int mob = DungeonPointBalance.mobReward(250.0, floor);
            int loss = DungeonPointBalance.deathLoss(100_000, floor);
            assertTrue(clear >= previousClear, "clear floor " + floor);
            assertTrue(boss >= previousBoss, "boss floor " + floor);
            assertTrue(mob >= previousMob, "mob floor " + floor);
            assertTrue(loss >= previousLoss, "death floor " + floor);
            previousClear = clear;
            previousBoss = boss;
            previousMob = mob;
            previousLoss = loss;
        }
    }
}
