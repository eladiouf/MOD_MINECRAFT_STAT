package tong.statmod.stats;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatMutationSyncSourceTest {
    private static final Path API_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "api", "STATModAPIImpl.java");
    private static final Path COMMAND_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "stats", "StatCommands.java");

    @Test
    void apiXpMutationsSyncStatsWhenCalledForServerPlayers() throws Exception {
        String source = Files.readString(API_SOURCE);

        assertTrue(source.contains("import net.minecraft.server.level.ServerPlayer;"),
                "API stat mutations need ServerPlayer access to send client sync packets");
        assertTrue(source.contains("player instanceof ServerPlayer serverPlayer"),
                "API stat mutations should only send network sync when the player is server-side");
        assertTrue(source.contains("LevelUpHandler.grantPendingPerkTiers(data);"),
                "API XP mutations should immediately apply newly reached global-level perk tiers");
        assertTrue(source.contains("SyncHelper.syncStats(serverPlayer);"),
                "API stat mutations must refresh the stat UI after changing XP");
        assertTrue(source.contains("SyncHelper.syncPerks(serverPlayer);"),
                "API XP mutations must refresh perk UI immediately when they grant new perk tiers");
    }

    @Test
    void apiRawXpUsesCentralProgressionPath() throws Exception {
        String source = Files.readString(API_SOURCE);
        String rawMethod = between(source,
                "public void addXpRaw",
                "private static void syncStatsIfServer");

        assertTrue(rawMethod.contains("RaceEffectApplier.addRawXp("),
                "raw API XP should still use the central progression path for level rewards and effective-level XP curve");
        assertFalse(rawMethod.contains("data.addXp("),
                "raw API XP should not bypass the central progression logic with direct attachment writes");
    }

    @Test
    void adminStatXpCommandSyncsStatsAfterGrantingXp() throws Exception {
        String source = Files.readString(COMMAND_SOURCE);
        String statXpCommand = between(source,
                "dispatcher.register(Commands.literal(\"statxp\")",
                "dispatcher.register(Commands.literal(\"statmod\")");

        assertTrue(statXpCommand.contains("instanceof ServerPlayer player"),
                "/statxp mutates server data and should operate on ServerPlayer for sync");
        assertTrue(statXpCommand.contains("LevelUpHandler.grantPendingPerkTiers(data);"),
                "/statxp should immediately apply newly reached global-level perk tiers");
        assertTrue(statXpCommand.contains("SyncHelper.syncStats(player);"),
                "/statxp must refresh the stat UI after XP changes");
        assertTrue(statXpCommand.contains("SyncHelper.syncPerks(player);"),
                "/statxp must refresh perk UI immediately when a new global tier is reached");
    }

    @Test
    void adminStatXpCommandUsesCentralRawProgressionPath() throws Exception {
        String source = Files.readString(COMMAND_SOURCE);
        String statXpCommand = between(source,
                "dispatcher.register(Commands.literal(\"statxp\")",
                "dispatcher.register(Commands.literal(\"statmod\")");

        assertTrue(statXpCommand.contains("RaceEffectApplier.addRawXp(player, index, amount, data);"),
                "/statxp should route through the shared raw-XP progression path");
        assertFalse(statXpCommand.contains("data.addXp(index, amount)"),
                "/statxp should not bypass level rewards and effective-level XP curve with direct attachment writes");
    }

    @Test
    void adminStatLevelCommandsRefreshPerksAfterCrossingGlobalTiers() throws Exception {
        String source = Files.readString(COMMAND_SOURCE);
        String statLevelCommand = between(source,
                "dispatcher.register(Commands.literal(\"statlevel\")",
                "dispatcher.register(Commands.literal(\"statxp\")");

        assertTrue(statLevelCommand.contains("RaceEffectApplier.addLevels(player, i, amount, data, false);"),
                "/statlevel all should use the shared direct-level progression path");
        assertTrue(statLevelCommand.contains("RaceEffectApplier.addLevels(player, index, amount, data, false);"),
                "/statlevel <index> should use the shared direct-level progression path");
        assertFalse(statLevelCommand.contains("data.addLevels(index, amount);"),
                "/statlevel <index> should not bypass shared level rewards with direct attachment writes");
    }

    @Test
    void adminStatCommandsDeriveIndexBoundsFromRuntimeStatCount() throws Exception {
        String source = Files.readString(COMMAND_SOURCE);

        assertTrue(source.contains("IntegerArgumentType.integer(0, PlayerStatData.STAT_COUNT - 1)"),
                "admin stat commands must accept the runtime stat index range");
        assertFalse(source.contains("IntegerArgumentType.integer(0, 22)"),
                "hardcoded stat index bounds desync when stats are added or removed");
    }

    @Test
    void adminStatPerkCommandSyncsPerksAfterGrantingPoints() throws Exception {
        String source = Files.readString(COMMAND_SOURCE);
        String statPerkCommand = source.substring(source.indexOf("dispatcher.register(Commands.literal(\"statperk\")"));

        assertTrue(statPerkCommand.contains("SyncHelper.syncPerks(player);"),
                "/statperk must refresh perk UI and Puffish mirrors after point changes");
    }

    @Test
    void magicPickraceCommandRefreshesProgressionStateImmediately() throws Exception {
        String source = Files.readString(COMMAND_SOURCE);
        String pickRaceCommand = between(source,
                ".then(Commands.literal(\"pickrace\")",
                "})))))");

        assertTrue(pickRaceCommand.contains("LevelUpHandler.grantPendingPerkTiers(data);"),
                "choosing a magic race can raise global level by unlocking magical stats, so perks must be granted immediately");
        assertTrue(pickRaceCommand.contains("SyncHelper.syncMagic(player);"),
                "pickrace must still refresh the magic UI");
        assertTrue(pickRaceCommand.contains("SyncHelper.syncStats(player);"),
                "pickrace must refresh stat UI because global level can change when magic stats become active");
        assertTrue(pickRaceCommand.contains("SyncHelper.syncPerks(player);"),
                "pickrace must refresh perk UI immediately when newly unlocked magical stats cross a global tier");
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, "Missing start token: " + startToken);
        assertTrue(end > start, "Missing end token after " + startToken + ": " + endToken);
        return source.substring(start, end);
    }
}
