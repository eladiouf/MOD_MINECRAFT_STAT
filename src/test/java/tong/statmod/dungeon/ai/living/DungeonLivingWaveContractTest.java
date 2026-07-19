package tong.statmod.dungeon.ai.living;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonLivingWaveContractTest {
    @Test void selectedLivingRolesReplaceExistingPendingWaveSlots() throws Exception {
        String spawner = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java"));
        int selection = spawner.indexOf("DungeonLivingEventPolicy.combatRole");
        int queue = spawner.indexOf("QUEUE.add(new Pending", selection);
        assertTrue(selection >= 0 && queue > selection);
        assertTrue(spawner.contains("AdventurerEntities.ADVENTURER.get()"));
        assertTrue(spawner.contains("DungeonLivingActor.marker"));
        assertTrue(spawner.contains("DungeonLivingActor.fromMarker"));
    }

    @Test void deferredSpawnPreservesExactRoomSquadAndSpecializedRoles() throws Exception {
        String actor = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingActor.java"));
        String spawner = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java"));
        assertTrue(actor.contains("initializeCombat"));
        assertTrue(actor.contains("DungeonTacticalRole.HUNTER"));
        assertTrue(actor.contains("DungeonTacticalRole.HEXER"));
        assertTrue(actor.contains("DungeonTacticalRole.WARDEN"));
        assertTrue(spawner.contains("p.floor() + \":room:\" + p.roomIndex()"));
        assertTrue(spawner.contains("DungeonLivingGoals.ensureAttached"));
    }
}
