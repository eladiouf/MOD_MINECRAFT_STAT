package tong.statmod.client.cosmetic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceCosmeticModelsSourceTest {
    @Test
    void headCosmeticAnchorsAreRaisedAboveDefaultHeadLine() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "cosmetic", "RaceCosmeticModels.java"));
        assertTrue(source.contains("PartPose.offsetAndRotation(4.0F, -7.0F"));
        assertTrue(source.contains("PartPose.offsetAndRotation(-4.0F, -7.0F"));
        assertTrue(source.contains("PartPose.offset(0.0F, -9.0F, 0.0F)"));
        assertTrue(source.contains("addBox(-3.0F, -3.0F, -4.5F"));
    }
}
