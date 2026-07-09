package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TempBuffLifecycleSourceTest {
    @Test
    void tempBuffLifecycleHandlerClearsTransientAwakeningStateOnPlayerLifecycle() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/tensura/TempBuffLifecycleHandler.java"));

        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"));
        assertTrue(source.contains("PlayerEvent.PlayerLoggedOutEvent"));
        assertTrue(source.contains("PlayerEvent.Clone"));
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"));
        assertTrue(source.contains("TempBuffManager.clear("));
    }

    @Test
    void statModRegistersTempBuffLifecycleHandlerWhenTensuraIsLoaded() throws IOException {
        Path path = Path.of("src/main/java/tong/statmod/STATMod.java");
        System.out.println("DEBUG WORKING DIR: " + Path.of(".").toAbsolutePath());
        System.out.println("DEBUG PATH RESOLVED: " + path.toAbsolutePath());
        String source = Files.readString(path);
        System.out.println("DEBUG SOURCE LENGTH: " + source.length());

        boolean ok = source.contains("NeoForge.EVENT_BUS.register(TempBuffLifecycleHandler.class);");
        if (!ok) {
            System.out.println("=== DEBUG SOURCE CONTENT ===");
            System.out.println(source);
            System.out.println("======================");
        }
        assertTrue(ok);
    }
}
