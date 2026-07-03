package tong.statmod.client.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tong.statmod.forge.EnchantmentAnvilRecipeCatalog;

import java.util.List;

public record EnchantmentAnvilJeiRecipe(
        ItemStack base,
        ItemStack primary,
        ItemStack secondary,
        ItemStack support,
        ItemStack result) {

    public static List<EnchantmentAnvilJeiRecipe> fromCatalog() {
        return EnchantmentAnvilRecipeCatalog.allRecipes().stream()
                .map(EnchantmentAnvilJeiRecipe::fromSpec)
                .filter(recipe -> !recipe.base().isEmpty()
                        && !recipe.primary().isEmpty()
                        && !recipe.support().isEmpty()
                        && !recipe.result().isEmpty())
                .toList();
    }

    private static EnchantmentAnvilJeiRecipe fromSpec(EnchantmentAnvilRecipeCatalog.RecipeSpec spec) {
        ItemStack base = stack(spec.baseId(), 1);
        ItemStack primary = stack(spec.primaryId(), spec.primaryCount());
        ItemStack secondary = spec.secondaryId().isBlank()
                ? ItemStack.EMPTY
                : stack(spec.secondaryId(), spec.secondaryCount());
        ItemStack support = stack(spec.supportId(), spec.supportCount());
        ItemStack result = EnchantmentAnvilRecipeCatalog.createResult(spec);
        return new EnchantmentAnvilJeiRecipe(base, primary, secondary, support, result);
    }

    private static ItemStack stack(String id, int count) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(key));
        stack.setCount(Math.max(count, 1));
        return stack;
    }
}
