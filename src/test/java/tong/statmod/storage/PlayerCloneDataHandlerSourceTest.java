package tong.statmod.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCloneDataHandlerSourceTest {
    @Test
    void cloneHandlerIsNotRegisteredOnNeoForgeEventBus_pushWasTestedAndReverted() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "STATMod.java"));

        assertTrue(source.contains("NeoForge.EVENT_BUS.register(StaminaEvents.class)"),
                "StaminaEvents should remain registered");
        assertTrue(!source.contains("PlayerCloneDataHandler"),
                "PlayerCloneDataHandler should be removed (NeoForge handles clone automatically)");
    }
}
