package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSpellPerkStateLifecycleSourceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "ironspells", "IronSpellEventBridge.java");

    @Test
    void ironSpellTransientCastStateIsClearedAcrossCloneAndRespawn() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PlayerEvent.Clone"),
                "IronSpellEventBridge should clear transient cast state on clone");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "IronSpellEventBridge should clear transient cast state on respawn");
        assertTrue(source.contains("IronSpellPerkState.clear"));
    }
}
