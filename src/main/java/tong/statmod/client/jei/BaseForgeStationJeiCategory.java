package tong.statmod.client.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

abstract class BaseForgeStationJeiCategory<T> implements IRecipeCategory<T> {

    private final RecipeType<T> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    protected BaseForgeStationJeiCategory(
            RecipeType<T> recipeType,
            IGuiHelper guiHelper,
            Component title,
            ItemStack iconStack,
            int width,
            int height) {
        this.recipeType = recipeType;
        this.title = title;
        this.background = guiHelper.createBlankDrawable(width, height);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }
}
