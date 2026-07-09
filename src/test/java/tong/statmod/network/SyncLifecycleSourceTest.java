package tong.statmod.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncLifecycleSourceTest {
    private static final Path STAT_MOD_SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "STATMod.java");
    private static final Path LIFECYCLE_SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "network", "SyncLifecycleHandler.java");

    @Test
    void statModRegistersSyncLifecycleHandler() throws IOException {
        String source = Files.readString(STAT_MOD_SOURCE);

        assertTrue(source.contains("NeoForge.EVENT_BUS.register(SyncLifecycleHandler.class);"),
                "STATMod should register the sync lifecycle handler on the main event bus");
    }

    @Test
    void lifecycleHandlerClearsPerPlayerSyncCachesAcrossCloneRespawnLoginAndLogout() throws IOException {
        String source = Files.readString(LIFECYCLE_SOURCE);

        assertTrue(source.contains("PlayerEvent.Clone"),
                "Sync lifecycle handler should reset cached sync snapshots on clone");
        assertTrue(source.contains("PlayerEvent.PlayerRespawnEvent"),
                "Sync lifecycle handler should reset cached sync snapshots on respawn");
        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"),
                "Sync lifecycle handler should reset cached sync snapshots on login");
        assertTrue(source.contains("PlayerEvent.PlayerLoggedOutEvent"),
                "Sync lifecycle handler should reset cached sync snapshots on logout");
        assertTrue(source.contains("SyncHelper.clearPlayerCache"),
                "Sync lifecycle handler should clear SyncHelper's per-player cache");
    }

    @Test
    void lifecycleHandlerResendsFullSnapshotsOnLoginAfterClearingGate() throws IOException {
        String source = Files.readString(LIFECYCLE_SOURCE);

        assertTrue(source.contains("PlayerEvent.PlayerLoggedInEvent"),
                "login hook must exist for reconnect recovery");
        assertTrue(source.contains("SyncHelper.syncAll(player);"),
                "login hook should resend stats, perks, stamina and magic after reconnect");
    }
}
