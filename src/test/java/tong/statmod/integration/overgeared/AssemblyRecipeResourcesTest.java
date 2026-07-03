package tong.statmod.integration.overgeared;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblyRecipeResourcesTest {

    private static final Path ASSEMBLY_DIR = Paths.get(
            "src", "main", "resources", "data", "statmod", "recipe", "assembly");

    @Test
    void assemblyRecipes_doNotTargetDescriptionPseudoItems() throws IOException {
        assertTrue(Files.exists(ASSEMBLY_DIR), "assembly recipes dir missing");

        try (Stream<Path> stream = Files.walk(ASSEMBLY_DIR)) {
            List<Path> recipes = stream
                    .filter(path -> path.toString().endsWith(".json"))
                    .toList();

            assertFalse(recipes.isEmpty(), "assembly recipes should exist");
            for (Path recipe : recipes) {
                String content = Files.readString(recipe);
                assertFalse(content.contains(".description_"),
                        "assembly recipe must target a real item id: " + recipe);
            }
        }
    }
}
