package tong.statmod.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SpellBindingKeyContractTest {
    @Test
    void jIsRegisteredOnceAndOnlySendsInGame() throws Exception {
        String mappings = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/ClientKeyMappings.java"));
        String input = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/ClientInputEvents.java"));

        assertTrue(mappings.contains("OPEN_SPELL_BINDING"));
        assertTrue(mappings.contains("GLFW.GLFW_KEY_J"));
        assertEquals(1, occurrences(mappings, "event.register(OPEN_SPELL_BINDING)"));
        assertTrue(input.contains("minecraft.player != null && minecraft.screen == null"));
        assertTrue(input.contains("StatNetwork.sendOpenSpellBinding()"));
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
