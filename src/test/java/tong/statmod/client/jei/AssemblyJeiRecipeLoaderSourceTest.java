package tong.statmod.client.jei;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblyJeiRecipeLoaderSourceTest {

    @Test
    void loaderFiltersAssemblyRecipesUsingForgeStationRules() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "jei", "AssemblyJeiRecipeLoader.java"));

        assertTrue(source.contains("getAllRecipesFor(RecipeType.CRAFTING)"));
        assertTrue(source.contains("holder.id().getPath().startsWith(\"assembly/\")"));
        assertTrue(source.contains("ForgeStationItemRules.isRoughIntermediateId"));
        assertTrue(source.contains("ForgeStationItemRules.isGripId"));
    }
}
