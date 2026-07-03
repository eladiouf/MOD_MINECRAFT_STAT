package tong.statmod.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import tong.statmod.block.ForgingBlocks;

@JeiPlugin
public final class StatModJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new InfusionForgeJeiCategory(registration.getJeiHelpers().getGuiHelper()),
                new InfusionForgeAssemblyJeiCategory(registration.getJeiHelpers().getGuiHelper()),
                new EnchantmentAnvilJeiCategory(registration.getJeiHelpers().getGuiHelper()),
                new OvergearedForgingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(StatModJeiRecipeTypes.INFUSION_FORGE, InfusionJeiRecipe.fromCatalog());
        registration.addRecipes(StatModJeiRecipeTypes.INFUSION_FORGE_ASSEMBLY, AssemblyJeiRecipeLoader.loadRecipes());
        registration.addRecipes(StatModJeiRecipeTypes.ENCHANTMENT_ANVIL, EnchantmentAnvilJeiRecipe.fromCatalog());
        registration.addRecipes(StatModJeiRecipeTypes.OVERGEARED_FORGING, OvergearedForgingJeiRecipeLoader.loadRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                ForgingBlocks.INFUSION_FORGE_ITEM.get(),
                StatModJeiRecipeTypes.INFUSION_FORGE,
                StatModJeiRecipeTypes.INFUSION_FORGE_ASSEMBLY);
        registration.addRecipeCatalyst(
                ForgingBlocks.ENCHANTMENT_ANVIL_ITEM.get(),
                StatModJeiRecipeTypes.ENCHANTMENT_ANVIL);
        for (ItemStack catalyst : OvergearedForgingJeiRecipeLoader.catalystStacks()) {
            registration.addRecipeCatalyst(catalyst, StatModJeiRecipeTypes.OVERGEARED_FORGING);
        }
    }
}
