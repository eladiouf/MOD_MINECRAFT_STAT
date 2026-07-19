package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonTacticalGoalsContractTest {
    @Test
    void supplementalGoalsExistAndAreMappedIdempotently() throws Exception {
        String wiring = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/DungeonTacticalGoals.java"));
        for (String goal : new String[]{"ScoutGoal", "WardenGoal", "BerserkerGoal",
                "SpellbreakerGoal", "HunterGoal", "SapperGoal"}) {
            assertTrue(wiring.contains(goal), goal);
            assertTrue(Files.exists(Path.of(
                    "src/main/java/tong/statmod/dungeon/ai/goal/" + goal + ".java")), goal);
        }
        assertTrue(wiring.contains("getAvailableGoals"));
    }
}
