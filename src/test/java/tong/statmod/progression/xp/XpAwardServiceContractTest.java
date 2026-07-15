package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
