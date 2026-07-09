package tong.statmod.integration.parcool;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcoolCacheLifecycleSourceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "parcool", "ParcoolAttributeHandler.java");

    @Test
    void parcoolCachesAreClearedAcrossPlayerLifecycle() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"),
                "ParcoolAttributeHandler should clear caches on login so stale UUID state never blocks reapply.");
        assertTrue(source.contains("PlayerEvent.PlayerLoggedOutEvent"),
                "ParcoolAttributeHandler should listen for logout to clear stale per-player caches");
        assertTrue(source.contains("PlayerEvent.Clone"),
                "ParcoolAttributeHandler should clear caches on clone because the new player entity reuses the same UUID.");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "ParcoolAttributeHandler should clear caches on respawn so modifiers are reapplied.");
        assertTrue(source.contains("lastMaxStamina.remove(uuid);"));
        assertTrue(source.contains("lastStaminaRecovery.remove(uuid);"));
        assertTrue(source.contains("lastAgility.remove(uuid);"));
    }
}
