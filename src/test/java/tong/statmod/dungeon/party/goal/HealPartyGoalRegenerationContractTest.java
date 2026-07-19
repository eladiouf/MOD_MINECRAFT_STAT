package tong.statmod.dungeon.party.goal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealPartyGoalRegenerationContractTest {
    @Test
    void normalHealAndSanctuaryUseReducedRegenerationOneDurations() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/party/goal/HealPartyGoal.java"));

        assertTrue(source.contains(
                "MobEffects.REGENERATION, 40, 0, false, true"));
        assertTrue(source.contains(
                "MobEffects.REGENERATION, 80, 0, false, true"));
        assertEquals(2, source.split("MobEffects.REGENERATION", -1).length - 1);
    }
}
