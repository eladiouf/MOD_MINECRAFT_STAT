package tong.statmod.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import tong.statmod.block.ForgingBlocks;

public final class EnchantmentAnvilJeiCategory extends BaseForgeStationJeiCategory<EnchantmentAnvilJeiRecipe> {

    public EnchantmentAnvilJeiCategory(IGuiHelper guiHelper) {
        super(
                StatModJeiRecipeTypes.ENCHANTMENT_ANVIL,
                guiHelper,
                Component.translatable("container.statmod.enchantment_anvil"),
                new ItemStack(ForgingBlocks.ENCHANTMENT_ANVIL_ITEM.get()),
                146,
                40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EnchantmentAnvilJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(3, 12)
                .addItemStack(recipe.base())
                .setStandardSlotBackground();
        builder.addInputSlot(30, 12)
                .addItemStack(recipe.primary())
                .setStandardSlotBackground();
        if (!recipe.secondary().isEmpty()) {
            builder.addInputSlot(57, 12)
                    .addItemStack(recipe.secondary())
                    .setStandardSlotBackground();
        }
        builder.addInputSlot(84, 12)
                .addItemStack(recipe.support())
                .setStandardSlotBackground();
        builder.addOutputSlot(120, 12)
                .addItemStack(recipe.result())
                .setOutputSlotBackground();
    }
}
