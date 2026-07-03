package tong.statmod.client.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record AssemblyJeiRecipe(
        ResourceLocation id,
        Ingredient base,
        Ingredient grip,
        ItemStack result) {
}
