package tong.statmod;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusSubscriberMigrationSourceTest {
    private static final Path STATMOD_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "STATMod.java");
    private static final Path CLIENT_SETUP_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "client", "ClientSetup.java");
    private static final Path CLIENT_INPUT_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "client", "ClientInputHandler.java");
    private static final Path RACE_COSMETIC_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "client", "cosmetic", "RaceCosmeticEvents.java");
    private static final Path NETWORK_HANDLER_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "network", "NetworkHandler.java");

    @Test
    void migratedHandlersDoNotUseDeprecatedBusSubscriberArgument() throws Exception {
        assertFalse(Files.readString(CLIENT_SETUP_SOURCE).contains("bus = EventBusSubscriber.Bus."));
        assertFalse(Files.readString(CLIENT_INPUT_SOURCE).contains("bus = EventBusSubscriber.Bus."));
        assertFalse(Files.readString(RACE_COSMETIC_SOURCE).contains("bus = EventBusSubscriber.Bus."));
        assertFalse(Files.readString(NETWORK_HANDLER_SOURCE).contains("bus = EventBusSubscriber.Bus."));
    }

    @Test
    void statModRegistersClientAndNetworkHandlersExplicitly() throws Exception {
        String source = Files.readString(STATMOD_SOURCE);

        assertTrue(source.contains("modBus.register(tong.statmod.network.NetworkHandler.class);"));
        assertTrue(source.contains("if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {"));
        assertTrue(source.contains("modBus.register(tong.statmod.client.ClientSetup.class);"));
        assertTrue(source.contains("modBus.register(tong.statmod.client.cosmetic.RaceCosmeticEvents.class);"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.register(tong.statmod.client.ClientInputHandler.class);"));
    }
}
