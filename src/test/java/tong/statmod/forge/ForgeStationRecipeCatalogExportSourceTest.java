package tong.statmod.forge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationRecipeCatalogExportSourceTest {

    @Test
    void infusionCatalogExposesRecipesForJei() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "forge", "InfusionForgeRecipeCatalog.java"));

        assertTrue(source.contains("public static List<RecipeSpec> allRecipes()"));
        assertTrue(source.contains("record RecipeSpec("));
    }

    @Test
    void enchantmentCatalogExposesRecipesForJei() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "forge", "EnchantmentAnvilRecipeCatalog.java"));

        assertTrue(source.contains("public static List<RecipeSpec> allRecipes()"));
    }
}
