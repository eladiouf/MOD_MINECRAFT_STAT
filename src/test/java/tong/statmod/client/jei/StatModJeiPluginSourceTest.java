package tong.statmod.client.jei;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatModJeiPluginSourceTest {

    @Test
    void pluginRegistersForgeCategoriesAndTheirCatalysts() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "jei", "StatModJeiPlugin.java"));

        assertTrue(source.contains("@JeiPlugin"));
        assertTrue(source.contains("implements IModPlugin"));
        assertTrue(source.contains("registerCategories"));
        assertTrue(source.contains("registerRecipes"));
        assertTrue(source.contains("registerRecipeCatalysts"));
        assertTrue(source.contains("StatModJeiRecipeTypes.INFUSION_FORGE"));
        assertTrue(source.contains("StatModJeiRecipeTypes.INFUSION_FORGE_ASSEMBLY"));
        assertTrue(source.contains("StatModJeiRecipeTypes.ENCHANTMENT_ANVIL"));
        assertTrue(source.contains("StatModJeiRecipeTypes.OVERGEARED_FORGING"));
        assertTrue(source.contains("ForgingBlocks.INFUSION_FORGE_ITEM"));
        assertTrue(source.contains("ForgingBlocks.ENCHANTMENT_ANVIL_ITEM"));
        assertTrue(source.contains("OvergearedForgingJeiRecipeLoader.loadRecipes()"));
        assertTrue(source.contains("OvergearedForgingJeiRecipeLoader.catalystStacks()"));
    }
}
