package tong.statmod.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingToolsResourcesTest {

    private static final Path RESOURCES = Paths.get("src", "main", "resources");

    @Test
    void forgingToolRecipesExist() {
        assertTrue(Files.exists(RESOURCES.resolve("data/statmod/recipe/assembly_components/basic_forge_tongs.json")));
        assertTrue(Files.exists(RESOURCES.resolve("data/statmod/recipe/assembly_components/basic_smithing_hammer.json")));
    }

    @Test
    void forgingToolItemModelsExist() {
        assertTrue(Files.exists(RESOURCES.resolve("assets/statmod/models/item/basic_forge_tongs.json")));
        assertTrue(Files.exists(RESOURCES.resolve("assets/statmod/models/item/basic_smithing_hammer.json")));
    }

    @Test
    void forgingToolLangEntriesExist() throws IOException {
        String en = Files.readString(RESOURCES.resolve("assets/statmod/lang/en_us.json"));
        String fr = Files.readString(RESOURCES.resolve("assets/statmod/lang/fr_fr.json"));

        assertTrue(en.contains("\"item.statmod.basic_forge_tongs\""));
        assertTrue(en.contains("\"item.statmod.basic_smithing_hammer\""));
        assertTrue(fr.contains("\"item.statmod.basic_forge_tongs\""));
        assertTrue(fr.contains("\"item.statmod.basic_smithing_hammer\""));
    }

    @Test
    void essenceRecipesReferenceForgeSupportTools() throws IOException {
        String katana = Files.readString(RESOURCES.resolve("data/statmod/recipe/essence/essence_flame_katana.json"));
        String claymore = Files.readString(RESOURCES.resolve("data/statmod/recipe/essence/essence_wither_claymore.json"));

        assertTrue(katana.contains("statmod:basic_forge_tongs"));
        assertTrue(claymore.contains("statmod:basic_smithing_hammer"));
    }
}
