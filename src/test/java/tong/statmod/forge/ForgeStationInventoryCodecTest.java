package tong.statmod.forge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationInventoryCodecTest {

    private static final Path JAVA_SRC = Paths.get("src", "main", "java", "tong", "statmod");

    @Test
    void blockEntities_useCodecAndUpdatedInputContainerSizes() throws IOException {
        String infusion = Files.readString(JAVA_SRC.resolve("block/entity/InfusionForgeBlockEntity.java"));
        String enchantment = Files.readString(JAVA_SRC.resolve("block/entity/EnchantmentAnvilBlockEntity.java"));

        assertTrue(infusion.contains("ForgeStationInventoryCodec.save"));
        assertTrue(infusion.contains("ForgeStationInventoryCodec.load"));
        assertTrue(infusion.contains("new SimpleContainer(InfusionForgeMenu.INPUT_SLOT_COUNT)"));

        assertTrue(enchantment.contains("ForgeStationInventoryCodec.save"));
        assertTrue(enchantment.contains("ForgeStationInventoryCodec.load"));
        assertTrue(enchantment.contains("new SimpleContainer(EnchantmentAnvilMenu.INPUT_SLOT_COUNT)"));
    }
}
