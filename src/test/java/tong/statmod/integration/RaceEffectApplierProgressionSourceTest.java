package tong.statmod.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceEffectApplierProgressionSourceTest {
    private static final Path SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "integration", "RaceEffectApplier.java");

    @Test
    void scaledXpAppliesPendingPerkTiersImmediatelyForServerPlayers() throws Exception {
        String source = Files.readString(SOURCE);
        String method = between(source,
                "public static boolean addScaledXp",
                "public static int scaleXpAmount");

        assertTrue(method.contains("LevelUpHandler.grantPendingPerkTiers(data);"),
                "scaled XP should immediately grant any newly reached global-tier perk points");
        assertTrue(method.contains("player instanceof net.minecraft.server.level.ServerPlayer serverPlayer"),
                "scaled XP should only send perk sync packets from the server side player instance");
        assertTrue(method.contains("SyncHelper.syncPerks(serverPlayer);"),
                "scaled XP should refresh perk UI and Puffish mirrors as soon as a tier is reached");
    }

    @Test
    void directLevelChangesAlsoRouteThroughSharedRewardLogic() throws Exception {
        String source = Files.readString(SOURCE);
        String method = between(source,
                "public static boolean addLevels",
                "public static int scaleXpAmount");

        assertTrue(method.contains("return addResolvedLevels("),
                "direct level changes should delegate into the shared progression helper");
        assertTrue(method.contains("StatLevelSkillRewards.grantReward(player, statIndex, level);"),
                "direct level gains should still grant milestone skill rewards");
        assertTrue(method.contains("LevelUpHandler.grantPendingPerkTiers(data);"),
                "direct level gains should still apply newly reached global-tier perk rewards");
        assertTrue(method.contains("SyncHelper.syncPerks(serverPlayer);"),
                "direct level gains should refresh perk UI immediately when they grant new global tiers");
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, "Missing start token: " + startToken);
        assertTrue(end > start, "Missing end token after " + startToken + ": " + endToken);
        return source.substring(start, end);
    }
}
