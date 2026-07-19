package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonEnemyHealthWiringContractTest {
    @Test
    void everyHostileSpawnAndRecoveryPathAppliesFinalHealthBalance() throws Exception {
        String scaling = dungeonSource("DungeonMobScaling.java");
        String parties = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/party/AdventurerPartyHelper.java"));
        String altar = dungeonSource("DungeonBossAltarBlock.java");
        String traps = dungeonSource("DungeonTrapHandler.java");
        String secrets = dungeonSource("DungeonSecretRoom.java");
        String vault = dungeonSource("DungeonUltraVault.java");
        String director = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java"));

        assertTrue(scaling.contains("DungeonEnemyHealthBalance.apply(mob)"));
        assertTrue(parties.contains("DungeonEnemyHealthBalance.apply(entity)"));
        assertTrue(altar.contains("DungeonEnemyHealthBalance.apply(livingBoss)"));
        assertTrue(traps.contains("DungeonEnemyHealthBalance.apply(ambusher)"));
        assertTrue(secrets.contains("DungeonEnemyHealthBalance.apply(secretBoss)"));
        assertTrue(vault.contains("DungeonEnemyHealthBalance.apply(vaultBoss)"));

        String hostileGuard = "if (!DungeonLivingActor.isNonCombat(actor))";
        int guardIndex = director.indexOf(hostileGuard);
        assertTrue(guardIndex >= 0);
        String hostileBranch = director.substring(
                guardIndex, Math.min(director.length(), guardIndex + 300));
        assertTrue(hostileBranch.contains("DungeonEnemyHealthBalance.apply(actor)"));
    }

    private static String dungeonSource(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/tong/statmod/dungeon", file));
    }
}
