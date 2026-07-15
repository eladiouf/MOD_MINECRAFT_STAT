package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CombatEffectEventsContractTest {
    @Test
    void usesOneForgeHurtAdapterAndExistingClassificationBoundaries() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/event/CombatEffectEvents.java"));

        assertTrue(source.contains("LivingHurtEvent"));
        assertTrue(source.contains("StatModServerConfig.snapshot()"));
        assertTrue(source.contains("WeaponClassifier.classify"));
        assertTrue(source.contains("CombatEligibility.eligibleTarget"));
        assertTrue(source.contains("CombatEligibility.physicalProfile"));
        assertTrue(source.contains("StatCapabilities.PLAYER_STATS"));
        assertEquals(1, occurrences(source, "event.setAmount("));
        assertFalse(source.contains("event.setCanceled("));
        assertFalse(source.contains("irons_spellbooks"));
        assertFalse(source.contains("epicfight"));
        assertFalse(source.toLowerCase().contains("tensura"));
    }

    private static int occurrences(String source, String token) {
        return source.split(Pattern.quote(token), -1).length - 1;
    }
}
