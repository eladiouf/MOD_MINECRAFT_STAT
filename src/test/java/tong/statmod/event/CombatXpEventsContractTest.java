package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatXpEventsContractTest {
    @Test
    void usesStandardForgeEventsAndDelegatesRewardCalculation() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/tong/statmod/event/CombatXpEvents.java"));

        assertTrue(source.contains("LivingDamageEvent"));
        assertTrue(source.contains("ShieldBlockEvent"));
        assertTrue(source.contains("LivingDeathEvent"));
        assertTrue(source.contains("priority = EventPriority.LOWEST"));
        assertTrue(source.contains("XpAwardService.award"));
        assertFalse(source.contains("* 2"));
        assertFalse(source.contains("StatType."));
        assertFalse(source.toLowerCase().contains("epicfight"));
        assertFalse(source.toLowerCase().contains("tensura"));
    }
}
