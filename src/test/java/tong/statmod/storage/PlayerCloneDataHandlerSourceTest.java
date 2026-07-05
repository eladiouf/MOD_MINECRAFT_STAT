package tong.statmod.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCloneDataHandlerSourceTest {
    @Test
    void cloneHandlerCopiesStatAndStaminaAttachmentsOnRespawn() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "storage", "PlayerCloneDataHandler.java"));

        assertTrue(source.contains("PlayerEvent.Clone"));
        assertTrue(source.contains("ModAttachments.STATS"));
        assertTrue(source.contains("ModAttachments.STAMINA"));
        assertTrue(source.contains("copyFrom"));
    }

    @Test
    void cloneHandlerIsRegisteredOnNeoForgeEventBus() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "STATMod.java"));

        assertTrue(source.contains("NeoForge.EVENT_BUS.register(PlayerCloneDataHandler.class)"));
    }

    @Test
    void respawnHandlerResyncsAllAttachmentsAfterRespawnPacketFlow() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "storage", "PlayerCloneDataHandler.java"));

        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"));
        assertTrue(source.contains("SyncHelper.syncAll(player);"));
    }
}
