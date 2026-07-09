package tong.statmod.perks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkStateLifecycleSourceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "perks", "PerkEffectHandler.java");

    @Test
    void perkRuntimeStateIsClearedAcrossCloneAndRespawn() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PlayerEvent.Clone"),
                "PerkEffectHandler should clear transient perk runtime state on clone");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "PerkEffectHandler should clear transient perk runtime state on respawn");
        assertTrue(source.contains("PerkState.clearPlayer(uuid);"));
    }
}
