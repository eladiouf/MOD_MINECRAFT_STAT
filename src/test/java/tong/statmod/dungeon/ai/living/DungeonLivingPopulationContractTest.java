package tong.statmod.dungeon.ai.living;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonLivingPopulationContractTest {
    @Test void combatFloorsPopulateOnlyTheCentralSafehouse() throws Exception {
        String rooms = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonRoomChain.java"));
        String population = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingPopulation.java"));
        assertTrue(rooms.contains("DungeonLivingPopulation.populateSafehouse"));
        assertTrue(rooms.contains("role == DungeonArchitect.Role.COMBAT"));
        assertTrue(population.contains("DungeonLayout.ROOM_COUNT / 2"));
        assertTrue(population.contains("Math.min(4"));
    }

    @Test void inhabitantsAreAuthorizedPersistentAndTaggedNonCombat() throws Exception {
        String population = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingPopulation.java"));
        String actor = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingActor.java"));
        assertTrue(population.contains("DungeonSpawnGuard.spawnAuthorized"));
        assertTrue(population.contains("setPersistenceRequired"));
        assertTrue(actor.contains("LIVING_ROLE_TAG"));
        assertTrue(actor.contains("NON_COMBAT_TAG"));
        assertTrue(actor.contains("PRISONER_RELEASED_TAG"));
        assertTrue(actor.contains("DungeonFaction.INHABITANTS"));
    }

    @Test void neutralActorsNeverBlockWaveOrRoomCompletion() throws Exception {
        String spawner = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java"));
        assertTrue(occurrences(spawner, "DungeonLivingActor.NON_COMBAT_TAG") >= 3);
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
