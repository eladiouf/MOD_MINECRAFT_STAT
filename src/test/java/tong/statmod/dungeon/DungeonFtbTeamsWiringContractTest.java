package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonFtbTeamsWiringContractTest {
    @Test
    void multipleTeamsShareEncountersButAssistsRemainTeamOnly() throws Exception {
        String teleport = source("DungeonTeleportHandler.java");
        String progress = source("DungeonProgress.java");
        String points = source("DungeonPoints.java");
        String respawn = source("DungeonRespawnHandler.java");
        String bosses = source("DungeonBossHandler.java");

        assertFalse(teleport.contains("DungeonFloorAdmission.canEnter"));
        assertFalse(teleport.contains("occupied_by_other_team"));
        assertTrue(progress.contains("DungeonTeleportHandler.playersOnFloor"));
        assertFalse(progress.contains("teammatesOnFloor"));
        assertTrue(points.contains("teammatesOnFloor"));
        assertTrue(respawn.contains("DungeonTeleportHandler.playersOnFloor"));
        assertFalse(respawn.contains("teammatesOnFloor"));
        assertTrue(bosses.contains("DungeonTeleportHandler.playersOnFloor"));
        assertFalse(bosses.contains("teammatesOnFloor"));
    }

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/tong/statmod/dungeon", file));
    }
}
