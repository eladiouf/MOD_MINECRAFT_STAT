package tong.statmod.client.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import net.stirdrem.overgeared.recipe.ModRecipeTypes;

import java.util.List;
import java.util.Optional;

public final class OvergearedForgingJeiRecipeLoader {

    private static final List<String> CATALYST_IDS = List.of(
            "overgeared:stone_anvil",
            "overgeared:smithing_anvil",
            "overgeared:tier_a_smithing_anvil",
            "overgeared:tier_b_smithing_anvil");

    private OvergearedForgingJeiRecipeLoader() {}

    public static List<OvergearedForgingJeiRecipe> loadRecipes() {
        RecipeManager recipeManager = connectedRecipeManager();
        if (recipeManager == null) {
            return List.of();
        }

        HolderLookup.Provider registries = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess()
                : null;
        if (registries == null) {
            return List.of();
        }

        return recipeManager.getAllRecipesFor(ModRecipeTypes.FORGING.get()).stream()
                .map(holder -> toRecipe(holder, registries))
                .flatMap(Optional::stream)
                .toList();
    }

    public static List<ItemStack> catalystStacks() {
        List<ItemStack> stacks = CATALYST_IDS.stream()
                .map(OvergearedForgingJeiRecipeLoader::stack)
                .filter(stack -> !stack.isEmpty())
                .toList();
        return stacks.isEmpty() ? List.of(new ItemStack(Items.ANVIL)) : stacks;
    }

    private static RecipeManager connectedRecipeManager() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null ? connection.getRecipeManager() : null;
    }

    private static Optional<OvergearedForgingJeiRecipe> toRecipe(
            RecipeHolder<ForgingRecipe> holder,
            HolderLookup.Provider registries) {
        ForgingRecipe recipe = holder.value();
        ItemStack result = recipe.getResultItem(registries);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new OvergearedForgingJeiRecipe(
                holder.id(),
                List.copyOf(recipe.getIngredients()),
                recipe.getWidth(),
                recipe.getHeight(),
                result,
                recipe.getAnvilTier(),
                recipe.getHammeringRequired()));
    }

    private static ItemStack stack(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(key);
        return new ItemStack(item);
    }
}
