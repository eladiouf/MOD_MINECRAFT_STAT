package tong.statmod.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceEffectApplierClientSyncSourceTest {

    private static final Path SOURCE = Paths.get("src", "main", "java",
            "tong", "statmod", "integration", "RaceEffectApplier.java");

    @Test
    void clientSideEffectiveLevelFallsBackToSyncedStatCache() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("player.level().isClientSide"),
                "client-side level resolution should detect logical client state");
        assertTrue(source.contains("ClientStatCache.getLevel(statIndex)"),
                "client-side level resolution should reuse the synced client stat cache");
    }

    @Test
    void clientSideBaseLevelDoesNotKeepStaleHigherAttachmentValues() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("ClientStatCache.hasLevel(statIndex)"),
                "client-side level resolution should only trust attachment fallback before the first cache sync");
        assertFalse(source.contains("Math.max(base, ClientStatCache.getLevel(statIndex))"),
                "taking the max keeps stale higher client attachment values after legitimate stat decreases");
    }
}
