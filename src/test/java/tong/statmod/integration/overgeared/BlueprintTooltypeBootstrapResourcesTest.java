package tong.statmod.integration.overgeared;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintTooltypeBootstrapResourcesTest {

    private static final Path STATMOD_FORGING_DIR = Paths.get(
            "src", "main", "resources", "data", "statmod", "recipe", "forging");
    private static final Path STATMOD_TOOLTYPE_DIR = Paths.get(
            "src", "main", "resources", "data", "statmod", "recipe", "item_to_tooltype");
    private static final Path OVERGEARED_TOOLTYPE_DIR = Paths.get(
            "src", "main", "resources", "data", "overgeared", "recipe", "item_to_tooltype");
    private static final Set<String> SUPPORTED_OVERGEARED_TOOLTYPES = Set.of(
            "sword", "axe", "pickaxe", "shovel", "hoe", "multitool");
    private static final Set<String> SUPPORTED_OVERGEARED_ANVIL_TIERS = Set.of(
            "stone", "iron", "above_a", "above_b");

    @Test
    void everyStatmodForgingBlueprintUsesASupportedOvergearedTooltype() throws IOException {
        for (ForgingRecipeSpec spec : loadForgingRecipeSpecs()) {
            for (String tooltype : spec.blueprintTypes()) {
                assertTrue(
                        SUPPORTED_OVERGEARED_TOOLTYPES.contains(tooltype),
                        "unsupported Overgeared blueprint type in " + spec.path() + ": " + tooltype);
            }
        }
    }

    @Test
    void everyStatmodForgingBlueprintHasABootstrapTooltypeRecipe() throws IOException {
        Set<String> blueprintTypes = new HashSet<>();
        for (ForgingRecipeSpec spec : loadForgingRecipeSpecs()) {
            blueprintTypes.addAll(spec.blueprintTypes());
        }

        for (String tooltype : blueprintTypes) {
            boolean exists =
                    Files.exists(STATMOD_TOOLTYPE_DIR.resolve(tooltype + ".json"))
                            || Files.exists(OVERGEARED_TOOLTYPE_DIR.resolve(tooltype + ".json"));
            assertTrue(exists, "missing bootstrap tooltype recipe for blueprint type: " + tooltype);
        }
    }

    @Test
    void everyStatmodForgingResultIsCoveredByItsBootstrapTooltypeRecipe() throws IOException {
        Map<String, Set<String>> tooltypeItems = loadTooltypeItems();

        for (ForgingRecipeSpec spec : loadForgingRecipeSpecs()) {
            for (String tooltype : spec.blueprintTypes()) {
                Set<String> items = tooltypeItems.get(tooltype);
                assertTrue(items != null && items.contains(spec.resultItemId()),
                        "missing tooltype mapping for result " + spec.resultItemId() + " -> " + tooltype);
            }
        }
    }

    @Test
    void everyStatmodForgingRecipeUsesASupportedOvergearedAnvilTier() throws IOException {
        for (ForgingRecipeSpec spec : loadForgingRecipeSpecs()) {
            assertTrue(
                    SUPPORTED_OVERGEARED_ANVIL_TIERS.contains(spec.anvilTier()),
                    "unsupported Overgeared anvil tier in " + spec.path() + ": " + spec.anvilTier());
        }
    }

    private static List<ForgingRecipeSpec> loadForgingRecipeSpecs() throws IOException {
        List<ForgingRecipeSpec> specs = new ArrayList<>();

        try (Stream<Path> files = Files.walk(STATMOD_FORGING_DIR)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                JsonArray blueprint = json.getAsJsonArray("blueprint");
                Set<String> blueprintTypes = new LinkedHashSet<>();
                for (int i = 0; i < blueprint.size(); i++) {
                    blueprintTypes.add(blueprint.get(i).getAsString());
                }
                JsonObject result = json.getAsJsonObject("result");
                specs.add(new ForgingRecipeSpec(
                        file,
                        blueprintTypes,
                        result.get("id").getAsString(),
                        json.get("tier").getAsString()));
            }
        }

        return specs;
    }

    private static Map<String, Set<String>> loadTooltypeItems() throws IOException {
        Map<String, Set<String>> byTooltype = new java.util.HashMap<>();
        loadTooltypeItemsFromDirectory(STATMOD_TOOLTYPE_DIR, byTooltype);
        loadTooltypeItemsFromDirectory(OVERGEARED_TOOLTYPE_DIR, byTooltype);
        return byTooltype;
    }

    private static void loadTooltypeItemsFromDirectory(Path dir, Map<String, Set<String>> byTooltype) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }

        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                String tooltype = json.get("tooltype").getAsString();
                Set<String> items = byTooltype.computeIfAbsent(tooltype, ignored -> new HashSet<>());
                JsonArray entries = json.getAsJsonArray("item");
                for (int i = 0; i < entries.size(); i++) {
                    JsonObject item = entries.get(i).getAsJsonObject();
                    if (item.has("item")) {
                        items.add(item.get("item").getAsString());
                    }
                }
            }
        }
    }

    private record ForgingRecipeSpec(Path path, Set<String> blueprintTypes, String resultItemId, String anvilTier) {}
}
