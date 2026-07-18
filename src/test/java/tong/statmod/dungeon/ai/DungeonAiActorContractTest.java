package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonAiActorContractTest {
    @Test
    void metadataDefinesPersistentIdentityAndSpawnBoundariesInitializeIt() throws Exception {
        String actor = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/DungeonAiActor.java"));
        for (String key : new String[]{"FACTION_TAG", "ALERT_TAG", "FLOOR_TAG", "SQUAD_TAG",
                "LAST_SEEN_X", "LAST_SEEN_Y", "LAST_SEEN_Z", "LAST_SEEN_TICK"}) {
            assertTrue(actor.contains(key), key);
        }
        String spawner = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java"));
        String boss = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java"));
        assertTrue(spawner.contains("DungeonAiActor.initialize"));
        assertTrue(boss.contains("DungeonAiActor.initialize"));
    }
}
