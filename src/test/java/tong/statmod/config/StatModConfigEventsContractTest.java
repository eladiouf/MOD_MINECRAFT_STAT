package tong.statmod.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StatModConfigEventsContractTest {
    @Test
    void refreshesConnectedPlayersOnStatModServerConfigReload() throws Exception {
        Path path = Path.of("src/main/java/tong/statmod/config/StatModConfigEvents.java");
        String source = Files.exists(path) ? Files.readString(path) : "";

        assertTrue(source.contains("ModConfigEvent.Reloading"));
        assertTrue(source.contains(
                "event.getConfig().getSpec() != StatModServerConfig.SPEC"));
        assertTrue(source.contains("ServerLifecycleHooks.getCurrentServer()"));
        assertTrue(source.contains("server.execute"));
        assertTrue(source.contains("getPlayerList().getPlayers()"));
        assertTrue(source.contains("PlayerAttributeEffects::refresh"));
    }
}
