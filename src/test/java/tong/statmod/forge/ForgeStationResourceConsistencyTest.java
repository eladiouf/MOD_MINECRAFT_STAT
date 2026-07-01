package tong.statmod.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationResourceConsistencyTest {

    private static final Path RECIPE_DIR = Paths.get(
            "src", "main", "resources", "data", "statmod", "recipe");
    private static final Path INFUSION_DIR = RECIPE_DIR.resolve("infusion");
    private static final Path ESSENCE_DIR = RECIPE_DIR.resolve("essence");
    private static final Path INFUSION_CATALOG = Paths.get(
            "src", "main", "java", "tong", "statmod", "forge", "InfusionForgeRecipeCatalog.java");
    private static final Path ENCHANTMENT_CATALOG = Paths.get(
            "src", "main", "java", "tong", "statmod", "forge", "EnchantmentAnvilRecipeCatalog.java");

    @Test
    void infusionAndEssenceRecipeDirsStillExistDuringMenuMigration() {
        assertTrue(Files.exists(INFUSION_DIR));
        assertTrue(Files.exists(ESSENCE_DIR));
    }

    @Test
    void infusionRecipesRemainRepresentedInInfusionCatalogSource() throws IOException {
        assertRecipesAreRepresented(Files.list(INFUSION_DIR).sorted().toList(), Files.readString(INFUSION_CATALOG));
    }

    @Test
    void essenceRecipesRemainRepresentedInEnchantmentCatalogSource() throws IOException {
        assertRecipesAreRepresented(Files.list(ESSENCE_DIR).sorted().toList(), Files.readString(ENCHANTMENT_CATALOG));
    }

    private static void assertRecipesAreRepresented(List<Path> recipes, String catalogSource) throws IOException {
        for (Path recipe : recipes) {
            JsonObject json = JsonParser.parseString(Files.readString(recipe)).getAsJsonObject();
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            String resultId = json.getAsJsonObject("result").get("id").getAsString();

            assertTrue(catalogSource.contains(resultId),
                    "missing result id from station catalog source: " + recipe.getFileName());

            for (int i = 0; i < ingredients.size(); i++) {
                String ingredientId = ingredients.get(i).getAsJsonObject().get("item").getAsString();
                assertTrue(catalogSource.contains(ingredientId),
                        "missing ingredient id from station catalog source: " + recipe.getFileName());
            }
        }
    }
}
