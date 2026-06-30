package tong.statmod.client.cosmetic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightRaceCosmeticLayerSourceTest {
    @Test
    void epicFightLayerPreparesPlayerModelBeforeHeadAndBodyAnchors() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "cosmetic", "EpicFightRaceCosmeticLayer.java"));
        assertTrue(source.contains("playerModel.prepareMobModel"));
        assertTrue(source.contains("playerModel.setupAnim"));
        assertTrue(source.contains("playerRenderer.getModel()"));
    }
}
