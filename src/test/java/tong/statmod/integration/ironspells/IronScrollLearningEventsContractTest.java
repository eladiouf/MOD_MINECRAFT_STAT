package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronScrollLearningEventsContractTest {
    @Test
    void everyScrollUseIsCanceledBeforeIronCastingAndOnlyServerConsumes() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronScrollLearningEvents.java"));

        assertTrue(source.contains("EventPriority.HIGHEST"));
        assertTrue(source.contains("instanceof IScroll"));
        assertTrue(source.contains("event.setCanceled(true)"));
        assertTrue(source.contains("event.setCancellationResult(InteractionResult.CONSUME)"));
        assertTrue(source.contains("event.getLevel().isClientSide()"));
        assertTrue(source.contains("outcome.consume() && !player.isCreative()"));
        assertTrue(source.contains("StatNetwork.sendSnapshot(player)"));
        assertFalse(source.contains("attemptInitiateCast"));
    }
}
