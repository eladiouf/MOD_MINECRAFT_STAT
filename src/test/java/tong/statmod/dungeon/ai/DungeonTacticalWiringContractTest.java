package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonTacticalWiringContractTest {
    @Test
    void rolesInitializeAtSpawnAndRecoverDuringDirectorCycles() throws Exception {
        String actor = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/DungeonAiActor.java"));
        String spawner = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java"));
        String boss = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java"));
        String director = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java"));
        assertTrue(actor.contains("TACTICAL_ROLE_TAG"));
        assertTrue(spawner.contains("DungeonTacticalRolePolicy.roleFor"));
        assertTrue(boss.contains("DungeonTacticalRolePolicy.roleFor"));
        assertTrue(director.contains("DungeonTacticalGoals.ensureAttached"));
    }
}
