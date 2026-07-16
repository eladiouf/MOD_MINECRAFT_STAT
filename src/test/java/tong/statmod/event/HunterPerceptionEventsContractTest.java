package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HunterPerceptionEventsContractTest {
    @Test
    void observesOnlyCommittedPositiveHostileDamageWithoutMutation() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/event/HunterPerceptionEvents.java"));

        assertTrue(source.contains("@SubscribeEvent(priority = EventPriority.LOWEST)"));
        assertTrue(source.contains("public static void damage(LivingDamageEvent event)"));
        assertTrue(source.contains("Float.isFinite(amount)"));
        assertTrue(source.contains("amount <= 0F"));
        assertTrue(source.contains("instanceof ServerPlayer player"));
        assertTrue(source.contains("target instanceof Enemy"));
        assertTrue(source.contains("XpAwardService.isEligible(player)"));
        assertTrue(source.contains("HunterTrackingService.mark(player, target)"));
        assertFalse(source.contains("event.setAmount("));
        assertFalse(source.contains("event.setCanceled("));
        assertFalse(source.contains("setGlowingTag("));
        assertFalse(source.contains("tong.statmod.dungeon"));
    }
}
