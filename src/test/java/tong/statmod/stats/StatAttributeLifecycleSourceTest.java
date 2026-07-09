package tong.statmod.stats;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatAttributeLifecycleSourceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "stats", "StatAttributeHandler.java");

    @Test
    void statAttributeCachesAreClearedAcrossPlayerLifecycle() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"),
                "StatAttributeHandler should clear stale attribute caches on login");
        assertTrue(source.contains("PlayerEvent.PlayerLoggedOutEvent"),
                "StatAttributeHandler should clear stale attribute caches on logout");
        assertTrue(source.contains("PlayerEvent.Clone"),
                "StatAttributeHandler should clear stale attribute caches on clone");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "StatAttributeHandler should clear stale attribute caches on respawn");
        assertTrue(source.contains("lastRapidite.remove(uuid);"));
        assertTrue(source.contains("lastAgility.remove(uuid);"));
    }
}
