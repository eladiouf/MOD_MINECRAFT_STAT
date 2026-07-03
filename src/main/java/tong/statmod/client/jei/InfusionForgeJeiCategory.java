package tong.statmod.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import tong.statmod.block.ForgingBlocks;

public final class InfusionForgeJeiCategory extends BaseForgeStationJeiCategory<InfusionJeiRecipe> {

    public InfusionForgeJeiCategory(IGuiHelper guiHelper) {
        super(
                StatModJeiRecipeTypes.INFUSION_FORGE,
                guiHelper,
                Component.translatable("container.statmod.infusion_forge"),
                new ItemStack(ForgingBlocks.INFUSION_FORGE_ITEM.get()),
                140,
                40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(7, 12)
                .addItemStack(recipe.base())
                .setStandardSlotBackground();
        builder.addInputSlot(43, 12)
                .addItemStack(recipe.essence())
                .setStandardSlotBackground();
        builder.addInputSlot(79, 12)
                .addItemStack(recipe.grip())
                .setStandardSlotBackground();
        builder.addOutputSlot(115, 12)
                .addItemStack(recipe.result())
                .setOutputSlotBackground();
    }
}
