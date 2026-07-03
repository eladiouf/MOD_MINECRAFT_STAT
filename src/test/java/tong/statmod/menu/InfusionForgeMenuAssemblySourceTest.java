package tong.statmod.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InfusionForgeMenuAssemblySourceTest {
    @Test
    void infusionForgeMenuFallsBackToAssemblyRecipesWhenEssenceSlotIsEmpty() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "menu", "InfusionForgeMenu.java"));

        assertTrue(source.contains("inputSlots.getItem(1).isEmpty()"));
        assertTrue(source.contains("RecipeType.CRAFTING"));
        assertTrue(source.contains("CraftingInput.of(2, 1"));
        assertTrue(source.contains("AssemblyForgingPolicy.canAssemble"));
    }
}
