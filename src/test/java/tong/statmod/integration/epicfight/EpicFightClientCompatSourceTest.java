package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightClientCompatSourceTest {
    @Test
    void epicFightCosmeticLayerUsesNativeRegistryHook() throws IOException {
        String compatSource = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "integration", "epicfight", "EpicFightClientCompat.java"));
        assertTrue(compatSource.contains("EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY"));
        assertTrue(compatSource.contains("addCustomLayer(new EpicFightRaceCosmeticLayer())"));

        String eventSource = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "cosmetic", "RaceCosmeticEvents.java"));
        assertFalse(eventSource.contains("RegisterPatchedRenderersEvent"));
    }
}
