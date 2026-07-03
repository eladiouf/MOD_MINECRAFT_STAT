package tong.statmod.client.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import tong.statmod.STATMod;
import tong.statmod.forge.ForgeStationItemRules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AssemblyJeiRecipeLoader {

    private static final Path DEV_ASSEMBLY_DIR = Path.of(
            "src", "main", "resources", "data", STATMod.MODID, "recipe", "assembly");

    private AssemblyJeiRecipeLoader() {}

    public static List<AssemblyJeiRecipe> loadRecipes() {
        RecipeManager recipeManager = connectedRecipeManager();
        if (recipeManager != null) {
            List<AssemblyJeiRecipe> recipes = fromRecipeManager(recipeManager);
            if (!recipes.isEmpty()) {
                return recipes;
            }
        }
        return fromDevSourceTree();
    }

    private static RecipeManager connectedRecipeManager() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null ? connection.getRecipeManager() : null;
    }

    private static List<AssemblyJeiRecipe> fromRecipeManager(RecipeManager recipeManager) {
        HolderLookup.Provider registries = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess()
                : null;
        if (registries == null) {
            return List.of();
        }

        return recipeManager.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(AssemblyJeiRecipeLoader::isAssemblyRecipe)
                .map(holder -> toRecipe(holder, registries))
                .flatMap(Optional::stream)
                .toList();
    }

    private static boolean isAssemblyRecipe(RecipeHolder<CraftingRecipe> holder) {
        return STATMod.MODID.equals(holder.id().getNamespace())
                && holder.id().getPath().startsWith("assembly/");
    }

    private static Optional<AssemblyJeiRecipe> toRecipe(
            RecipeHolder<CraftingRecipe> holder,
            HolderLookup.Provider registries) {
        List<Ingredient> ingredients = holder.value().getIngredients();
        if (ingredients.size() != 2) {
            return Optional.empty();
        }

        Ingredient first = ingredients.get(0);
        Ingredient second = ingredients.get(1);
        Ingredient base;
        Ingredient grip;
        if (matchesSingleItem(first, ForgeStationItemRules::isRoughIntermediateId)
                && matchesSingleItem(second, ForgeStationItemRules::isGripId)) {
            base = first;
            grip = second;
        } else if (matchesSingleItem(second, ForgeStationItemRules::isRoughIntermediateId)
                && matchesSingleItem(first, ForgeStationItemRules::isGripId)) {
            base = second;
            grip = first;
        } else {
            return Optional.empty();
        }

        ItemStack result = holder.value().assemble(CraftingInput.of(2, 1, List.of(
                oneItem(base),
                oneItem(grip))), registries);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AssemblyJeiRecipe(holder.id(), base, grip, result));
    }

    private static List<AssemblyJeiRecipe> fromDevSourceTree() {
        if (!Files.isDirectory(DEV_ASSEMBLY_DIR)) {
            return List.of();
        }

        try (Stream<Path> files = Files.walk(DEV_ASSEMBLY_DIR)) {
            return files
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(AssemblyJeiRecipeLoader::parseJsonRecipe)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static Optional<AssemblyJeiRecipe> parseJsonRecipe(Path path) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            if (ingredients.size() != 2) {
                return Optional.empty();
            }

            String firstId = ingredients.get(0).getAsJsonObject().get("item").getAsString();
            String secondId = ingredients.get(1).getAsJsonObject().get("item").getAsString();
            String baseId;
            String gripId;
            if (ForgeStationItemRules.isRoughIntermediateId(firstId)
                    && ForgeStationItemRules.isGripId(secondId)) {
                baseId = firstId;
                gripId = secondId;
            } else if (ForgeStationItemRules.isRoughIntermediateId(secondId)
                    && ForgeStationItemRules.isGripId(firstId)) {
                baseId = secondId;
                gripId = firstId;
            } else {
                return Optional.empty();
            }

            ItemStack result = stack(
                    json.getAsJsonObject("result").get("id").getAsString(),
                    json.getAsJsonObject("result").get("count").getAsInt());
            if (result.isEmpty()) {
                return Optional.empty();
            }

            Item baseItem = item(baseId);
            Item gripItem = item(gripId);
            if (baseItem == null || gripItem == null) {
                return Optional.empty();
            }

            Path relative = DEV_ASSEMBLY_DIR.relativize(path);
            String pathId = relative.toString()
                    .replace('\\', '/')
                    .replaceAll("\\.json$", "");
            return Optional.of(new AssemblyJeiRecipe(
                    ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "assembly/" + pathId),
                    Ingredient.of(baseItem),
                    Ingredient.of(gripItem),
                    result));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static boolean matchesSingleItem(Ingredient ingredient, Predicate<String> matcher) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 1 && matcher.test(ForgeStationItemRules.itemId(items[0]));
    }

    private static ItemStack oneItem(Ingredient ingredient) {
        return ingredient.getItems()[0].copyWithCount(1);
    }

    private static Item item(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        return BuiltInRegistries.ITEM.containsKey(key) ? BuiltInRegistries.ITEM.get(key) : null;
    }

    private static ItemStack stack(String id, int count) {
        Item item = item(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        stack.setCount(Math.max(count, 1));
        return stack;
    }
}
