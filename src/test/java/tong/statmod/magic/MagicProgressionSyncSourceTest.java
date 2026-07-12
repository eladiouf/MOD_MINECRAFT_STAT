package tong.statmod.magic;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicProgressionSyncSourceTest {
    private static final Path COMBAT_XP_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "progression", "CombatXPHandler.java");
    private static final Path IRON_EVENT_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "integration", "ironspells", "IronSpellEventBridge.java");

    @Test
    void combatMagicPointRewardsRefreshMagicState() throws Exception {
        String source = Files.readString(COMBAT_XP_SOURCE);
        String onKill = between(source,
                "public static void onKill",
                "public static Player resolveAttacker");

        assertTrue(onKill.contains("data.addMagicPoints(mp);"),
                "combat kill rewards should still grant magic points");
        assertTrue(onKill.contains("SyncHelper.syncMagic((ServerPlayer) player);"),
                "combat kill magic point rewards must refresh the magic UI and Puffish mirror");
    }

    @Test
    void ironSpellCastRewardsRefreshMagicState() throws Exception {
        String source = Files.readString(IRON_EVENT_SOURCE);
        String onPostCast = between(source,
                "public static void onPostCast",
                "public static void onSpellDamage");

        assertTrue(source.contains("import tong.statmod.network.SyncHelper;"),
                "Iron spell bridge should use the central magic sync helper");
        assertTrue(onPostCast.contains("if (reward.practiceMasteryDelta() > 0) SchoolProgressTracker.applyPracticeMastery(data, branch, reward.practiceMasteryDelta());"),
                "void-cast practice must be routed to the non-bankable mastery channel");
        assertTrue(onPostCast.contains("if (reward.progressionMasteryDelta() > 0) SchoolProgressTracker.applyMastery(data, branch, reward.progressionMasteryDelta());"),
                "impact mastery must remain on the bankable progression channel");
        assertTrue(onPostCast.contains("boolean magicChanged = reward.practiceMasteryDelta() > 0 || reward.progressionMasteryDelta() > 0 || reward.magicPointsDelta() > 0;"),
                "cast rewards should sync either practice or bankable progression changes");
        assertTrue(onPostCast.contains("if (magicChanged) SyncHelper.syncMagic(player);"),
                "cast rewards must refresh magic state after mastery or point changes");
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, "Missing start token: " + startToken);
        assertTrue(end > start, "Missing end token after " + startToken + ": " + endToken);
        return source.substring(start, end);
    }
}
