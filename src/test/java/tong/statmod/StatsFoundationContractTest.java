package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StatsFoundationContractTest {
    @Test
    void entrypointRegistersNetworkAndLifecycleOwnsRequiredEvents() throws Exception {
        String entrypoint = Files.readString(Path.of("src/main/java/tong/statmod/StatMod.java"));
        String events = Files.readString(
                Path.of("src/main/java/tong/statmod/event/PlayerStatsEvents.java"));

        assertTrue(entrypoint.contains("StatNetwork.register()"));
        assertTrue(events.contains("PlayerLoggedInEvent"));
        assertTrue(events.contains("PlayerRespawnEvent"));
        assertTrue(events.contains("PlayerChangedDimensionEvent"));
        assertTrue(events.contains("RegisterCommandsEvent"));
    }

    @Test
    void coreSourceHasNoThirdPartyIntegrationImports() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            String sources = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        String normalized = path.toString().replace('\\', '/');
                        return !normalized.contains("/integration/ironspells/")
                                && !normalized.contains("/integration/sdmshop/")
                                && !normalized.contains("/mixin/")
                                && !normalized.contains("/client/inscription/");
                    })
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .reduce("", String::concat);

            assertFalse(sources.toLowerCase().lines()
                    .map(String::strip)
                    .filter(line -> line.startsWith("import "))
                    .anyMatch(line -> line.contains("epicfight")
                            || line.contains("ironsspellbooks")
                            || line.contains("irons_spellbooks")
                            || line.contains("tensura")
                            || line.contains("parcool")));
        }
    }
}
