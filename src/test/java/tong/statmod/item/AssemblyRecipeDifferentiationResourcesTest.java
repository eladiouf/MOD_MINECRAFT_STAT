package tong.statmod.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblyRecipeDifferentiationResourcesTest {

    private static final Path ASSEMBLY = Paths.get("src", "main", "resources", "data", "statmod", "recipe", "assembly");
    private static final Set<String> FORM_COMPONENTS = Set.of(
            "statmod:katana_tsuba",
            "statmod:rapier_guard",
            "statmod:claymore_pommel",
            "statmod:halberd_socket",
            "statmod:warhammer_core",
            "statmod:staff_focus"
    );
    private static final Set<String> GRIPS = Set.of(
            "statmod:wooden_grip",
            "statmod:leather_wrap",
            "statmod:wire_wrap",
            "statmod:runic_grip"
    );
    private static final Set<String> ACCENTS = Set.of(
            "minecraft:bamboo",
            "minecraft:gold_nugget",
            "minecraft:flint",
            "minecraft:iron_nugget",
            "minecraft:iron_ingot",
            "minecraft:stick",
            "minecraft:copper_ingot",
            "minecraft:amethyst_shard",
            "minecraft:prismarine_shard",
            "minecraft:string"
    );

    @Test
    void generatedAssemblyRecipes_useAtLeastFiveIngredientsWithGripFormComponentAndAccent() throws IOException {
        try (Stream<Path> stream = Files.walk(ASSEMBLY)) {
            for (Path recipePath : stream.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject recipe = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
                JsonArray ingredients = recipe.getAsJsonArray("ingredients");

                assertTrue(ingredients.size() >= 5, recipePath + " should require at least 5 ingredients");
                assertTrue(containsAnyItem(ingredients, GRIPS), recipePath + " should require a grip family item");
                assertTrue(containsAnyItem(ingredients, FORM_COMPONENTS),
                        recipePath + " should require a weapon-form component");
                assertTrue(containsAnyItem(ingredients, ACCENTS),
                        recipePath + " should require a silhouette accent ingredient");
            }
        }
    }

    @Test
    void representativeWeaponFamilies_mapToDistinctAssemblyComponents() throws IOException {
        assertRecipeContains("simplyswords/diamond_katana.json", "statmod:katana_tsuba", "statmod:leather_wrap", "minecraft:bamboo");
        assertRecipeContains("simplyswords/diamond_rapier.json", "statmod:rapier_guard", "statmod:leather_wrap", "minecraft:gold_nugget");
        assertRecipeContains("simplyswords/diamond_claymore.json", "statmod:claymore_pommel", "statmod:wire_wrap", "minecraft:copper_ingot");
        assertRecipeContains("simplyswords/diamond_halberd.json", "statmod:halberd_socket", "statmod:wooden_grip", "minecraft:iron_ingot");
        assertRecipeContains("simplyswords/diamond_greathammer.json", "statmod:warhammer_core", "statmod:wire_wrap", "minecraft:copper_ingot");
        assertRecipeContains("irons_spellbooks/pyrium_staff.json", "statmod:staff_focus", "statmod:wooden_grip", "minecraft:amethyst_shard");
        assertRecipeContains("magistuarmory/diamond_pike.json", "statmod:halberd_socket", "statmod:wooden_grip", "minecraft:iron_nugget");
        assertRecipeContains("epicfight/diamond_dagger.json", "statmod:rapier_guard", "statmod:leather_wrap", "minecraft:flint");
        assertRecipeIngredientCountAtLeast("simplyswords/diamond_longsword.json", "statmod:rough_blade_diamond", 2);
        assertRecipeIngredientCountAtLeast("simplyswords/diamond_claymore.json", "statmod:rough_blade_diamond", 2);
        assertRecipeIngredientCountAtLeast("simplyswords/diamond_claymore.json", "statmod:claymore_pommel", 2);
        assertRecipeIngredientCountAtLeast("epicfight_dd/diamond_battlestaff.json", "statmod:staff_focus", 2);
    }

    @Test
    void generatedAssemblyRecipes_areGloballyUniqueByIngredientMultiset() throws IOException {
        Set<String> seenPatterns = new HashSet<>();

        try (Stream<Path> stream = Files.walk(ASSEMBLY)) {
            for (Path recipePath : stream.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject recipe = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
                JsonArray ingredients = recipe.getAsJsonArray("ingredients");
                Map<String, Integer> counts = new TreeMap<>();
                for (int i = 0; i < ingredients.size(); i++) {
                    String itemId = ingredients.get(i).getAsJsonObject().get("item").getAsString();
                    counts.merge(itemId, 1, Integer::sum);
                }
                String key = counts.toString();
                assertTrue(seenPatterns.add(key),
                        recipePath + " duplicates an existing assembly multiset: " + key);
            }
        }
    }

    @Test
    void assemblyGenerator_sourceDeclaresFormComponents() throws IOException {
        String source = Files.readString(Paths.get("tools", "generate_assembly_recipes.py"));

        assertTrue(source.contains("katana_tsuba"));
        assertTrue(source.contains("rapier_guard"));
        assertTrue(source.contains("claymore_pommel"));
        assertTrue(source.contains("halberd_socket"));
        assertTrue(source.contains("warhammer_core"));
        assertTrue(source.contains("staff_focus"));
        assertTrue(source.contains("minecraft:bamboo"));
        assertTrue(source.contains("minecraft:gold_nugget"));
        assertTrue(source.contains("minecraft:flint"));
        assertTrue(source.contains("minecraft:iron_nugget"));
        assertTrue(source.contains("minecraft:iron_ingot"));
        assertTrue(source.contains("minecraft:stick"));
        assertTrue(source.contains("minecraft:copper_ingot"));
        assertTrue(source.contains("minecraft:amethyst_shard"));
        assertTrue(source.contains("minecraft:prismarine_shard"));
        assertTrue(source.contains("minecraft:string"));
    }

    private static boolean containsAnyItem(JsonArray ingredients, Set<String> expectedItems) {
        for (int i = 0; i < ingredients.size(); i++) {
            JsonObject ingredient = ingredients.get(i).getAsJsonObject();
            if (ingredient.has("item") && expectedItems.contains(ingredient.get("item").getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static void assertRecipeContains(String relativePath, String componentId, String gripId, String accentId) throws IOException {
        String content = Files.readString(ASSEMBLY.resolve(relativePath));
        assertTrue(content.contains(componentId), relativePath + " should include " + componentId);
        assertTrue(content.contains(gripId), relativePath + " should include " + gripId);
        assertTrue(content.contains(accentId), relativePath + " should include " + accentId);
    }

    private static void assertRecipeIngredientCountAtLeast(String relativePath, String itemId, int expectedMinimum) throws IOException {
        JsonObject recipe = JsonParser.parseString(Files.readString(ASSEMBLY.resolve(relativePath))).getAsJsonObject();
        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        int count = 0;
        for (int i = 0; i < ingredients.size(); i++) {
            JsonObject ingredient = ingredients.get(i).getAsJsonObject();
            if (ingredient.has("item") && itemId.equals(ingredient.get("item").getAsString())) {
                count++;
            }
        }
        assertTrue(count >= expectedMinimum,
                relativePath + " should use " + itemId + " at least " + expectedMinimum + " times, got " + count);
    }
}
