package tong.statmod.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerPayloadHandlerSyncSourceTest {
    private static final Path SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "network", "ServerPayloadHandler.java");

    @Test
    void unlockPerkUsesCentralPerkSyncSoPuffishMirrorsRefresh() throws Exception {
        String source = Files.readString(SOURCE);
        String handler = between(source,
                "public static void handleUnlockPerk",
                "public static void handleUnlockMagicNode");

        assertTrue(handler.contains("SyncHelper.syncPerks(player);"),
                "perk unlocks must use the central sync helper so STATMod UI and Puffish mirrors update together");
        assertFalse(handler.contains("new SyncPerksPayload"),
                "direct perk payload sends bypass Puffish mirror sync");
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, "Missing start token: " + startToken);
        assertTrue(end > start, "Missing end token after " + startToken + ": " + endToken);
        return source.substring(start, end);
    }
}
