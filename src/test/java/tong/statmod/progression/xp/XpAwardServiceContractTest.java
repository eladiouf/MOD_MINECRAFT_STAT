package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XpAwardServiceContractTest {
    @Test
    void synchronizesExactlyOnceAndHasNoOptionalModDependency() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/progression/xp/XpAwardService.java"));

        assertEquals(1, occurrences(source, "StatNetwork.sendSnapshot(player)"));
        assertFalse(source.toLowerCase().contains("epicfight"));
        assertFalse(source.toLowerCase().contains("tensura"));
    }

    @Test
    void creativePermissionIsRestrictedToTheSpellCastEntryPoint() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/progression/xp/XpAwardService.java"));

        String general = method(source, "public static boolean isEligible(",
                "public static boolean award(");
        assertTrue(general.contains("!player.isCreative()"));
        assertTrue(general.contains("!player.isSpectator()"));
        assertTrue(general.contains("!(player instanceof FakePlayer)"));

        String spell = method(source, "public static boolean awardSpellCast(",
                "private static boolean awardEligible(");
        assertTrue(spell.contains("action.kind() != XpActionKind.SPELL_CAST"));
        assertTrue(spell.contains("player instanceof FakePlayer"));
        assertTrue(spell.contains("player.isSpectator()"));
        assertFalse(spell.contains("player.isCreative()"));
        assertTrue(spell.contains("awardEligible(player, List.of(action), tick)"));
    }

    private static String method(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from);
        return source.substring(from, to);
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
