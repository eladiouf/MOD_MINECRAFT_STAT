package tong.statmod.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import tong.statmod.block.ForgingBlocks;

public final class InfusionForgeAssemblyJeiCategory extends BaseForgeStationJeiCategory<AssemblyJeiRecipe> {

    public InfusionForgeAssemblyJeiCategory(IGuiHelper guiHelper) {
        super(
                StatModJeiRecipeTypes.INFUSION_FORGE_ASSEMBLY,
                guiHelper,
                Component.translatable("jei.statmod.infusion_forge_assembly"),
                new ItemStack(ForgingBlocks.INFUSION_FORGE_ITEM.get()),
                140,
                40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblyJeiRecipe recipe, IFocusGroup focuses) {
        builder.setShapeless();
        builder.addInputSlot(25, 12)
                .addIngredients(recipe.base())
                .setStandardSlotBackground();
        builder.addInputSlot(61, 12)
                .addIngredients(recipe.grip())
                .setStandardSlotBackground();
        builder.addOutputSlot(115, 12)
                .addItemStack(recipe.result())
                .setOutputSlotBackground();
    }
}
