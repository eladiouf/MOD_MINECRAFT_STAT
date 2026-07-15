package tong.statmod.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StatsScreenWiringContractTest {
    @Test
    void keyAndInputUseForgeClientEvents() throws Exception {
        String keys = source("client/ClientKeyMappings.java");
        String input = source("client/ClientInputEvents.java");

        assertTrue(keys.contains("GLFW.GLFW_KEY_P"));
        assertTrue(keys.contains("RegisterKeyMappingsEvent"));
        assertTrue(keys.contains("event.register(OPEN_STATS)"));
        assertTrue(input.contains("TickEvent.ClientTickEvent"));
        assertTrue(input.contains("Phase.END"));
        assertTrue(input.contains("consumeClick"));
        assertTrue(input.contains("setScreen(new StatsOverviewScreen())"));
    }

    @Test
    void screenIsReadOnlyAndSupportsExpectedNavigation() throws Exception {
        String screen = source("client/stats/StatsOverviewScreen.java");

        assertTrue(screen.contains("mouseScrolled"));
        assertTrue(screen.contains("OPEN_STATS.matches"));
        assertTrue(screen.contains("keyInventory.matches"));
        assertTrue(screen.contains("ClientStatsCache.state()"));
        assertTrue(screen.contains("StatCardRenderer.narration(card)"));
        assertFalse(screen.contains("StatNetwork"));
        assertFalse(screen.contains("sendToServer"));
        assertFalse(screen.contains("StatCapabilities"));
    }

    private static String source(String relative) throws Exception {
        Path path = Path.of("src/main/java/tong/statmod").resolve(relative);
        return Files.exists(path) ? Files.readString(path) : "";
    }
}
