package tong.statmod.world.effect;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class AdrenalineBrewingRecipe implements IBrewingRecipe {
    private final Potion input;
    private final Item ingredient;
    private final Potion output;

    public AdrenalineBrewingRecipe(Potion input, Item ingredient, Potion output) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }

    @Override
    public boolean isInput(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        return stack.is(ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;
        Potion inputPotion = PotionUtils.getPotion(input);
        if (inputPotion != this.input) return ItemStack.EMPTY;
        ItemStack result = input.copy();
        result.setCount(1);
        PotionUtils.setPotion(result, this.output);
        return result;
    }
}
