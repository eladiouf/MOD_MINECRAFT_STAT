package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class HunterTrackingServiceContractTest {
    @Test
    void resolvesTrackingDurationAndSendsOnePersonalMark() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/effects/HunterTrackingService.java"));

        assertTrue(source.contains("StatType.TRACKING"));
        assertTrue(source.contains("AutomaticPerkBonuses.from(stats)"));
        assertTrue(source.contains("AutomaticPerkEffect.TRACKING_FOCUS"));
        assertTrue(source.contains("HunterPerceptionRules.trackingDurationTicks"));
        assertTrue(source.contains("duration == 0"));
        assertTrue(source.contains("target instanceof Enemy"));
        assertTrue(source.contains("target.isAlive()"));
        assertEquals(1, occurrences(source, "StatNetwork.sendTrackedPrey("));
        assertFalse(source.contains("setGlowingTag("));
        assertFalse(source.contains("tong.statmod.dungeon"));
        assertFalse(source.contains("static final Map"));
    }

    private static int occurrences(String source, String token) {
        return source.split(Pattern.quote(token), -1).length - 1;
    }
}
