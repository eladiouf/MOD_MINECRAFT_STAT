package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListMixinInitialSyncSourceTest {
    private static final Path SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "mixin", "PlayerListMixin.java");

    @Test
    void initialLoginSyncIncludesStatsPerksStaminaAndMagic() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("new BatchSyncPayload("),
                "login sync should keep the existing stats/perks batch payload");
        assertTrue(source.contains("SyncHelper.syncStats(player);"),
                "login sync must also send a full stat snapshot so dungeon fields are initialized");
        assertTrue(source.contains("SyncHelper.syncStamina(player);"),
                "login sync must initialize the client stamina cache before combat starts");
        assertTrue(source.contains("SyncHelper.syncMagic(player);"),
                "login sync must initialize the client magic cache");
        assertTrue(source.contains("PuffishSkillsCompat.sync(player, data);"),
                "login sync must refresh Puffish perk mirrors");
    }
}
