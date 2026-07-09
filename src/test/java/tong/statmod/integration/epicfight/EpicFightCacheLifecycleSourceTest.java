package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightCacheLifecycleSourceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "epicfight", "EpicFightCompat.java");

    @Test
    void epicFightCachesAreClearedAcrossPlayerLifecycle() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"),
                "EpicFightCompat should clear caches on login so stale UUID state never blocks reapply.");
        assertTrue(source.contains("PlayerEvent.PlayerLoggedOutEvent"),
                "EpicFightCompat should listen for logout to clear stale per-player caches");
        assertTrue(source.contains("PlayerEvent.Clone"),
                "EpicFightCompat should clear caches on clone because the new player entity reuses the same UUID.");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "EpicFightCompat should clear caches on respawn so modifiers are reapplied.");
        assertTrue(source.contains("lastWeight.remove(uuid);"));
        assertTrue(source.contains("lastStunArmor.remove(uuid);"));
        assertTrue(source.contains("lastImpact.remove(uuid);"));
        assertTrue(source.contains("lastReach.remove(uuid);"));
        assertTrue(source.contains("lastServerMaxStamina.remove(uuid);"));
        assertTrue(source.contains("lastClientMaxStamina.remove(uuid);"));
    }
}
