package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSpellAttributeLifecycleSourceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "ironspells", "IronSpellAttributeBridge.java");

    @Test
    void ironSpellAttributeCacheIsResetAcrossPlayerLifecycle() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PlayerEvent.Clone"),
                "IronSpellAttributeBridge should clear stale snapshots on player clone");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "IronSpellAttributeBridge should clear stale snapshots on respawn");
        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"),
                "IronSpellAttributeBridge should refresh snapshots on login");
        assertTrue(source.contains("LAST_APPLIED.remove(event.getEntity().getUUID());"));
    }
}
