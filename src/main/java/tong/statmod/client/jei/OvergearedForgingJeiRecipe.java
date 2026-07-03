package tong.statmod.client.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record OvergearedForgingJeiRecipe(
        ResourceLocation id,
        List<Ingredient> ingredients,
        int width,
        int height,
        ItemStack result,
        String anvilTier,
        int hammeringRequired) {
}
