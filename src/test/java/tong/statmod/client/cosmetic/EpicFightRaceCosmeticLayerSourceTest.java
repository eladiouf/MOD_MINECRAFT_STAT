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

    @Test
    void epicFightLayerAnchorsCosmeticsThroughRootJointHeightCorrection() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "cosmetic", "EpicFightRaceCosmeticLayer.java"));
        assertTrue(source.contains("searchJointByName(\"Root\")"));
        assertTrue(source.contains("MathUtils.mulStack"));
        assertTrue(source.contains("PLAYER_LAYER_HEIGHT_CORRECTION = 0.75D"));
        assertTrue(source.contains("poseStack.translate(0.0D, PLAYER_LAYER_HEIGHT_CORRECTION, 0.0D)"));
        assertTrue(source.contains("poseStack.scale(-1.0F, -1.0F, 1.0F)"));
    }
}
