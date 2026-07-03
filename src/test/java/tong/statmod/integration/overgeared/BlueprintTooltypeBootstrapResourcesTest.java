package tong.statmod.integration.overgeared;

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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintTooltypeBootstrapResourcesTest {

    private static final Path STATMOD_FORGING_DIR = Paths.get(
            "src", "main", "resources", "data", "statmod", "recipe", "forging");
    private static final Path STATMOD_TOOLTYPE_DIR = Paths.get(
            "src", "main", "resources", "data", "statmod", "recipe", "item_to_tooltype");
    private static final Path OVERGEARED_TOOLTYPE_DIR = Paths.get(
            "src", "main", "resources", "data", "overgeared", "recipe", "item_to_tooltype");

    @Test
    void everyStatmodForgingBlueprintHasABootstrapTooltypeRecipe() throws IOException {
        Set<String> blueprintTypes = new HashSet<>();

        try (Stream<Path> files = Files.walk(STATMOD_FORGING_DIR)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                JsonArray blueprint = json.getAsJsonArray("blueprint");
                for (int i = 0; i < blueprint.size(); i++) {
                    blueprintTypes.add(blueprint.get(i).getAsString());
                }
            }
        }

        for (String tooltype : blueprintTypes) {
            boolean exists =
                    Files.exists(STATMOD_TOOLTYPE_DIR.resolve(tooltype + ".json"))
                            || Files.exists(OVERGEARED_TOOLTYPE_DIR.resolve(tooltype + ".json"));
            assertTrue(exists, "missing bootstrap tooltype recipe for blueprint type: " + tooltype);
        }
    }
}
