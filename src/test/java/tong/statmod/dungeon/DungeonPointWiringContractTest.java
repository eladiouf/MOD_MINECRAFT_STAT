package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonPointWiringContractTest {
    @Test
    void everyServerRewardAndPenaltyReceivesTheActualFloor() throws Exception {
        String points = source("DungeonPoints.java");
        String progress = source("DungeonProgress.java");
        String respawn = source("DungeonRespawnHandler.java");

        assertTrue(points.contains("DungeonPointBalance.mobReward(difficultyRating(mob), floor)"));
        assertTrue(points.contains("DungeonPointBalance.floorClearReward(floor)"));
        assertTrue(points.contains("DungeonPointBalance.bossReward(floor)"));
        assertTrue(points.contains("DungeonPointBalance.deathLoss(current, floor)"));
        assertTrue(points.contains("DungeonPointBalance.assistShare(basePoints)"));
        assertFalse(points.contains("FLOOR_CLEAR_POINTS"));
        assertFalse(points.contains("BOSS_POINTS"));
        assertFalse(points.contains("DEATH_LOSS_FRACTION"));

        assertTrue(progress.contains("DungeonPoints.awardBoss(player, floor,"));
        assertTrue(progress.contains("DungeonPoints.awardFloorClear(player, floor,"));
        assertTrue(respawn.contains("DungeonPoints.applyDeathPenalty(player, floor)"));
    }

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/tong/statmod/dungeon", file));
    }
}
