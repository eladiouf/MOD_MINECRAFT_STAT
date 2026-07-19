package tong.statmod.client.hunter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ClientHunterPerceptionContractTest {
    @Test
    void ticksBoundedPersonalMarkAndCrouchedThreatScan() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/hunter/ClientHunterPerception.java"));
        String input = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/ClientInputEvents.java"));
        String events = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/ClientStatsEvents.java"));

        assertTrue(input.contains("ClientHunterPerception.tick(minecraft)"));
        assertTrue(events.contains("ClientHunterPerception.clear()"));
        assertTrue(source.contains("SCAN_INTERVAL_TICKS = 5"));
        assertTrue(source.contains("MAX_THREATS = 64"));
        assertTrue(source.contains("player.isCrouching()"));
        assertTrue(source.contains("player.isSpectator()"));
        assertTrue(source.contains("player.isAlive()"));
        assertTrue(source.contains("lastLevel != level"));
        assertTrue(source.contains("ClientStatsCache.state()"));
        assertTrue(source.contains("StatType.TRACKING"));
        assertTrue(source.contains("StatType.KEEN_SENSES"));
        assertTrue(source.contains("HunterPerceptionRules.trackingRangeBlocks"));
        assertTrue(source.contains("HunterPerceptionRules.keenSensesRangeBlocks"));
        assertTrue(source.contains("instanceof Enemy"));
        assertTrue(source.contains("HunterThreatSelection.select"));
        assertTrue(source.contains("List.copyOf"));
        assertTrue(source.contains("statmod:tracking_25"));
        assertTrue(source.contains("statmod:tracking_50"));
        assertTrue(source.contains("statmod:tracking_75"));
        assertTrue(source.contains("statmod:keen_senses_25"));
        assertTrue(source.contains("statmod:keen_senses_50"));
        assertTrue(source.contains("statmod:keen_senses_75"));
        assertFalse(source.contains("setGlowingTag("));
        assertFalse(source.contains("tong.statmod.dungeon"));
    }
}
