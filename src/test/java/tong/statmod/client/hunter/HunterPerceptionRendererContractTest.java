package tong.statmod.client.hunter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HunterPerceptionRendererContractTest {
    @Test
    void rendersPrivateContoursAfterEntitiesAndRestoresDepth() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/hunter/HunterPerceptionRenderer.java"));

        assertTrue(source.contains("value = Dist.CLIENT"));
        assertTrue(source.contains("RenderLevelStageEvent.Stage.AFTER_ENTITIES"));
        assertTrue(source.contains("LevelRenderer.renderLineBox"));
        assertTrue(source.contains("RenderSystem.disableDepthTest()"));
        assertTrue(source.contains("RenderSystem.enableDepthTest()"));
        assertTrue(source.contains("AMBER_RED = 1.0F"));
        assertTrue(source.contains("AMBER_GREEN = 0.62F"));
        assertTrue(source.contains("AMBER_BLUE = 0.10F"));
        assertTrue(source.contains("THREAT_RED = 1.0F"));
        assertTrue(source.contains("THREAT_GREEN = 0.15F"));
        assertTrue(source.contains("THREAT_BLUE = 0.15F"));
        assertTrue(source.contains("if (entityId == markedId)"));
        assertTrue(source.contains("event.getCamera().getPosition()"));
        assertTrue(source.contains("Mth.lerp"));
        assertFalse(source.contains("setGlowingTag("));
        assertFalse(source.contains("ClientboundSetEntityDataPacket"));
        assertFalse(source.contains("PlayerTeam"));
        assertFalse(source.contains("playSound"));
        assertFalse(source.contains("sendParticles"));
        assertFalse(source.contains("displayClientMessage"));
    }
}
