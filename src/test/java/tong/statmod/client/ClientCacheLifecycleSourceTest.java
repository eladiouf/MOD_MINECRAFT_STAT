package tong.statmod.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientCacheLifecycleSourceTest {
    @Test
    void lifecycleOnlyClearsClientCachesOnLogoutSoLoginSyncCannotBeWiped() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "ClientCacheLifecycle.java"));

        assertTrue(source.contains("ClientPlayerNetworkEvent.LoggingOut"));
        assertTrue(!source.contains("ClientPlayerNetworkEvent.LoggingIn"),
                "login-side cache resets can wipe the initial reconnect sync");
        assertTrue(source.contains("ClientStatCache.reset();"));
        assertTrue(source.contains("ClientPerkCache.reset();"));
        assertTrue(source.contains("ClientStaminaCache.reset();"));
        assertTrue(source.contains("ClientMagicCache.reset();"));
    }

    @Test
    void lifecycleUsesClientGameEventBusSubscriber() throws IOException {
        String lifecycleSource = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "ClientCacheLifecycle.java"));
        String setupSource = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "ClientSetup.java"));

        assertTrue(lifecycleSource.contains("@SubscribeEvent"));
        assertTrue(setupSource.contains("NeoForge.EVENT_BUS.register(ClientCacheLifecycle.class);"));
    }
}
