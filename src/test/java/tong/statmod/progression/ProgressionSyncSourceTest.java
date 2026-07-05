package tong.statmod.progression;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionSyncSourceTest {
    private static final Path LEVEL_UP_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "progression", "LevelUpHandler.java");
    private static final Path SOUL_LEVEL_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "integration", "SoulLevelSyncHandler.java");
    private static final Path DUNGEON_PROGRESS_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "dungeon", "DungeonProgress.java");

    @Test
    void automaticPerkPointGrantsRefreshPerkState() throws Exception {
        String source = Files.readString(LEVEL_UP_SOURCE);

        assertTrue(source.contains("PerkPointAllocator.grantPointsToAllFamilies(data, granted);"),
                "global-level tier rewards should still grant points to every perk family");
        assertTrue(source.contains("player instanceof ServerPlayer serverPlayer"),
                "tier rewards should only send network sync when running for a server player");
        assertTrue(source.contains("SyncHelper.syncPerks(serverPlayer);"),
                "automatic perk point grants must refresh the client perk UI and Puffish mirrors");
    }

    @Test
    void soulLevelChangesRefreshStatState() throws Exception {
        String source = Files.readString(SOUL_LEVEL_SOURCE);

        assertTrue(source.contains("data.setSoulLevel(tensuraSoul);"),
                "Tensura soul level should still update STATMod's persisted soulLevel");
        assertTrue(source.contains("player instanceof ServerPlayer serverPlayer"),
                "soul-level changes should only send network sync when running for a server player");
        assertTrue(source.contains("SyncHelper.syncStats(serverPlayer);"),
                "soul-level changes must refresh stat UI because StatUpdatePayload carries soulLevel");
    }

    @Test
    void dungeonBossStatRewardsAlsoRefreshPerkStateImmediately() throws Exception {
        String source = Files.readString(DUNGEON_PROGRESS_SOURCE);

        assertTrue(source.contains("RaceEffectApplier.addLevels(player, statIndex, gain, data, true);"),
                "direct dungeon boss stat gains should use the shared direct-level progression path");
        assertTrue(source.contains("SyncHelper.syncStats(player);"),
                "dungeon boss stat gains must still refresh the stat UI after the shared progression path runs");
    }
}
