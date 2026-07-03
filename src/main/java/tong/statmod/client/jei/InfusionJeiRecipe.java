package tong.statmod.client.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tong.statmod.forge.InfusionForgeRecipeCatalog;

import java.util.List;

public record InfusionJeiRecipe(ItemStack base, ItemStack essence, ItemStack grip, ItemStack result) {

    public static List<InfusionJeiRecipe> fromCatalog() {
        return InfusionForgeRecipeCatalog.allRecipes().stream()
                .map(InfusionJeiRecipe::fromSpec)
                .filter(recipe -> !recipe.base().isEmpty()
                        && !recipe.essence().isEmpty()
                        && !recipe.grip().isEmpty()
                        && !recipe.result().isEmpty())
                .toList();
    }

    private static InfusionJeiRecipe fromSpec(InfusionForgeRecipeCatalog.RecipeSpec spec) {
        return new InfusionJeiRecipe(
                stack(spec.baseId()),
                stack(spec.essenceId()),
                stack(spec.gripId()),
                stack(spec.resultId()));
    }

    private static ItemStack stack(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(key));
    }
}
