package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class StatItemTagResourcesTest {
    @Test
    void shipsAllPublicClassificationTagsWithExpectedDefaults() {
        assertTag("heavy_weapons", Set.of("#minecraft:axes"));
        assertTag("blade_weapons", Set.of("#minecraft:swords"));
        assertTag("precision_weapons", Set.of(
                "minecraft:bow", "minecraft:crossbow", "minecraft:trident"));
        assertTag("forgeable_equipment", Set.of(
                "#minecraft:axes", "#minecraft:swords", "#minecraft:pickaxes",
                "#minecraft:shovels", "#minecraft:hoes", "#minecraft:trimmable_armor",
                "minecraft:bow", "minecraft:crossbow", "minecraft:trident",
                "minecraft:shield"));
    }

    private static void assertTag(String name, Set<String> expected) {
        String path = "/data/statmod/tags/items/" + name + ".json";
        InputStream stream = StatItemTagResourcesTest.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing resource " + path);
        JsonObject root = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(root.get("replace").getAsBoolean());
        JsonArray values = root.getAsJsonArray("values");
        Set<String> actual = StreamSupport.stream(values.spliterator(), false)
                .map(value -> value.getAsString()).collect(Collectors.toSet());
        assertTrue(actual.containsAll(expected));
    }
}
