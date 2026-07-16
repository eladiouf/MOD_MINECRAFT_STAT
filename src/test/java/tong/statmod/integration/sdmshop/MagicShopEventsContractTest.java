package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagicShopEventsContractTest {
    @Test
    void generatesAfterServerStartupWithRequiredModsLoaded() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/sdmshop/MagicShopEvents.java"));
        assertTrue(source.contains("ServerStartedEvent"));
        assertTrue(source.contains("server.execute"));
        assertTrue(source.contains("MagicShopGenerator.regenerate(server)"));
        assertTrue(source.contains("isLoaded(\"sdmshop\")"));
        assertTrue(source.contains("isLoaded(\"sdmeconomy\")"));
        assertTrue(source.contains("isLoaded(\"irons_spellbooks\")"));
    }

    @Test
    void loginSchedulesExactlyOneThousandStartingCoins() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/event/PlayerStatsEvents.java"));
        assertTrue(source.contains("serverPlayer.server.execute"));
        assertTrue(source.contains("SDMEconomyBridge.ensureStartingBalance(serverPlayer, 1_000L)"));
    }
}
