package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonFtbTeamsWiringContractTest {
    @Test
    void everyCooperativeRecipientPathUsesFtbTeams() throws Exception {
        String teleport = source("DungeonTeleportHandler.java");
        String progress = source("DungeonProgress.java");
        String points = source("DungeonPoints.java");
        String respawn = source("DungeonRespawnHandler.java");
        String bosses = source("DungeonBossHandler.java");

        assertTrue(teleport.contains("DungeonFloorAdmission.canEnter"));
        assertTrue(teleport.contains("FTBTeamsBridge.loaded()"));
        assertTrue(progress.contains("teammatesOnFloor"));
        assertTrue(points.contains("teammatesOnFloor"));
        assertTrue(respawn.contains("teammatesOnFloor"));
        assertTrue(bosses.contains("teammatesOnFloor"));
    }

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/tong/statmod/dungeon", file));
    }
}
