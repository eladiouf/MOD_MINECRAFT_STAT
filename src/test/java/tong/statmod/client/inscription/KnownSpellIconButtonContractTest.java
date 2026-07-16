package tong.statmod.client.inscription;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KnownSpellIconButtonContractTest {
    @Test
    void buttonUsesRealIconLearnedLevelAndBoundMarker() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/inscription/KnownSpellIconButton.java"));

        assertTrue(source.contains("spell.getSpellIconResource()"));
        assertTrue(source.contains("learnedLevel"));
        assertTrue(source.contains("statmod.spell.tooltip.bound"));
        assertTrue(source.contains("0xFF55E060"));
    }
}
