package tong.statmod.loot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatLootModifierDataTest {

    @Test
    void statLootModifierData_existsAndTargetsRegisteredCodec() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
            .getResourceAsStream("data/statmod/loot_modifiers/stat_loot.json")) {
            assertNotNull(stream, "Missing loot modifier data resource for statmod:stat_loot");

            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            assertEquals("statmod:stat_loot", root.get("type").getAsString());
            assertTrue(root.getAsJsonArray("conditions").isEmpty());
        }
    }
}
