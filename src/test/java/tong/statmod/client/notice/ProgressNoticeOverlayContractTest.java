package tong.statmod.client.notice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProgressNoticeOverlayContractTest {
    @Test
    void rendersOnlyCompactSubduedLevelNotices() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/notice/ProgressNoticeOverlay.java"));

        assertTrue(source.contains("WIDTH = 160"));
        assertTrue(source.contains("HEIGHT = 20"));
        assertTrue(source.contains("GAP = 3"));
        assertTrue(source.contains("BACKGROUND_ALPHA = 110"));
        assertTrue(source.contains("BORDER_ALPHA = 150"));
        assertTrue(source.contains("TEXT_ALPHA = 200"));
        assertTrue(source.contains("notice.statmod.level_up"));
        assertFalse(source.contains("notice.statmod.xp"));
    }
}
