package tong.statmod.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class OvergearedForgingJeiCategory extends BaseForgeStationJeiCategory<OvergearedForgingJeiRecipe> {

    public OvergearedForgingJeiCategory(IGuiHelper guiHelper) {
        super(
                StatModJeiRecipeTypes.OVERGEARED_FORGING,
                guiHelper,
                Component.translatable("jei.statmod.overgeared_forging"),
                OvergearedForgingJeiRecipeLoader.catalystStacks().getFirst(),
                158,
                58);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OvergearedForgingJeiRecipe recipe, IFocusGroup focuses) {
        int startX = 8;
        int startY = 2;
        int slotSize = 18;

        for (int index = 0; index < recipe.ingredients().size(); index++) {
            if (recipe.ingredients().get(index).isEmpty()) {
                continue;
            }

            int x = startX + (index % Math.max(recipe.width(), 1)) * slotSize;
            int y = startY + (index / Math.max(recipe.width(), 1)) * slotSize;
            builder.addInputSlot(x, y)
                    .addIngredients(recipe.ingredients().get(index))
                    .setStandardSlotBackground();
        }

        builder.addOutputSlot(132, 20)
                .addItemStack(recipe.result())
                .setOutputSlotBackground();
    }
}
