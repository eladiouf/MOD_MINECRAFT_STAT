package tong.statmod.item;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingFormComponentsResourcesTest {

    private static final Path RESOURCES = Paths.get("src", "main", "resources");

    @Test
    void formComponentTexturesExist() {
        assertTrue(Files.exists(RESOURCES.resolve("assets/statmod/textures/item/halberd_socket.png")));
        assertTrue(Files.exists(RESOURCES.resolve("assets/statmod/textures/item/warhammer_core.png")));
    }

    @Test
    void formComponentModelsUseDedicatedTextures() throws Exception {
        String halberdSocket = Files.readString(RESOURCES.resolve("assets/statmod/models/item/halberd_socket.json"));
        String warhammerCore = Files.readString(RESOURCES.resolve("assets/statmod/models/item/warhammer_core.json"));

        assertTrue(halberdSocket.contains("statmod:item/halberd_socket"));
        assertTrue(warhammerCore.contains("statmod:item/warhammer_core"));
    }
}
