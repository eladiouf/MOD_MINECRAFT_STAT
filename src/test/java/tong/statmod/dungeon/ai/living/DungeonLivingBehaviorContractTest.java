package tong.statmod.dungeon.ai.living;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonLivingBehaviorContractTest {
    @Test void directorRecoversGoalsAndNeverAggrosInhabitants() throws Exception {
        String director = read("src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java");
        String goals = read("src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingGoals.java");
        assertTrue(director.contains("DungeonLivingGoals.ensureAttached"));
        assertTrue(director.contains("DungeonFaction.INHABITANTS"));
        assertTrue(director.contains("actor.setTarget(null)"));
        assertTrue(goals.contains("getAvailableGoals"));
    }

    @Test void prisonerReleaseIsExplicitPersistentAndPlayerOwned() throws Exception {
        String events = read("src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingInteractionEvents.java");
        String follow = read("src/main/java/tong/statmod/dungeon/ai/living/goal/PrisonerFollowGoal.java");
        assertTrue(events.contains("PlayerInteractEvent.EntityInteract"));
        assertTrue(events.contains("PRISONER_RELEASED_TAG"));
        assertTrue(events.contains("PRISONER_OWNER_TAG"));
        assertTrue(follow.contains("PRISONER_OWNER_TAG"));
        assertTrue(follow.contains("getPlayerByUUID"));
    }

    @Test void scavengerApproachesItemsWithoutDeletingProtectedLoot() throws Exception {
        String source = read("src/main/java/tong/statmod/dungeon/ai/living/goal/ScavengerGoal.java");
        assertTrue(source.contains("ItemEntity"));
        assertTrue(source.contains("moveTo"));
        assertFalse(source.contains("discard()"));
        assertFalse(source.contains("remove("));
    }

    @Test void engineerRepairsOnlyExactSquadConstructsOnBudget() throws Exception {
        String source = read("src/main/java/tong/statmod/dungeon/ai/living/goal/EngineerRepairGoal.java");
        assertTrue(source.contains("squadId.equals"));
        assertTrue(source.contains("DungeonFaction.DUNGEON_CONSTRUCTS"));
        assertTrue(source.contains("REPAIR_COOLDOWN = 100"));
        assertTrue(source.contains("heal("));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
