package tong.statmod.client.jei;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OvergearedForgingJeiRecipeLoaderSourceTest {

    @Test
    void loaderReadsOvergearedForgingRecipeTypeFromRecipeManager() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "jei", "OvergearedForgingJeiRecipeLoader.java"));

        assertTrue(source.contains("ModRecipeTypes.FORGING.get()"));
        assertTrue(source.contains("getAllRecipesFor"));
        assertTrue(source.contains("ForgingRecipe"));
        assertTrue(source.contains("OvergearedForgingJeiRecipe"));
        assertTrue(source.contains("overgeared:smithing_anvil"));
    }
}
