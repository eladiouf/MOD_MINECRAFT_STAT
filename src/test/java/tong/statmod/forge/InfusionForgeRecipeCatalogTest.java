package tong.statmod.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfusionForgeRecipeCatalogTest {

    private static final Path INFUSION_DIR = Paths.get(
            "src", "main", "resources", "data", "statmod", "recipe", "infusion");

    @Test
    void catalog_matchesKnownInfusionRecipes() {
        assertEquals("simplyswords:runic_rapier",
                InfusionForgeRecipeCatalog.resultIdForIds(
                        "statmod:rough_dagger_blade_arcane",
                        "statmod:rune_essence_arcane",
                        "statmod:runic_grip"));
        assertEquals("simplyswords:runic_katana",
                InfusionForgeRecipeCatalog.resultIdForIds(
                        "statmod:rough_blade_arcane",
                        "statmod:rune_essence_arcane",
                        "statmod:runic_grip"));
    }

    @Test
    void infusionRecipes_areUnambiguousForStationInputs() throws IOException {
        Set<String> signatures = new HashSet<>();

        for (Path recipe : Files.list(INFUSION_DIR).toList()) {
            JsonObject json = JsonParser.parseString(Files.readString(recipe)).getAsJsonObject();
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            String signature = ingredients.get(0).getAsJsonObject().get("item").getAsString()
                    + "|"
                    + ingredients.get(1).getAsJsonObject().get("item").getAsString()
                    + "|"
                    + ingredients.get(2).getAsJsonObject().get("item").getAsString();
            assertTrue(signatures.add(signature),
                    "duplicate station signature for infusion recipe: " + recipe.getFileName());
        }
    }
}
