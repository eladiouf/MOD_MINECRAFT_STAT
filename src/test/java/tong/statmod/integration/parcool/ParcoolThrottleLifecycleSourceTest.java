package tong.statmod.integration.parcool;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcoolThrottleLifecycleSourceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "parcool", "ParcoolCompat.java");

    @Test
    void parcoolEnduranceThrottleIsClearedAcrossPlayerLifecycle() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"),
                "ParcoolCompat should clear stale endurance throttles on login");
        assertTrue(source.contains("PlayerEvent.PlayerLoggedOutEvent"),
                "ParcoolCompat should clear stale endurance throttles on logout");
        assertTrue(source.contains("PlayerEvent.Clone"),
                "ParcoolCompat should clear stale endurance throttles on clone");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "ParcoolCompat should clear stale endurance throttles on respawn");
        assertTrue(source.contains("lastEnduranceGain.remove(uuid);"));
    }
}
