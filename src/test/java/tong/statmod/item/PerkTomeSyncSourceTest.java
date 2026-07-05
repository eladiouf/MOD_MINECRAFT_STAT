package tong.statmod.item;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkTomeSyncSourceTest {
    private static final Path SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "item", "PerkTomeItem.java");

    @Test
    void perkTomeRefreshesPerkStateAfterGrantingPoints() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("player instanceof net.minecraft.server.level.ServerPlayer serverPlayer"),
                "perk tome should only send sync packets from the server side player instance");
        assertTrue(source.contains("SyncHelper.syncPerks(serverPlayer);"),
                "perk tome must refresh perk UI and Puffish mirrors immediately after granting points");
    }
}
