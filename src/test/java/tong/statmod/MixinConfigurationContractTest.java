package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MixinConfigurationContractTest {
    @Test
    void configRegistersOnlyTheTwoInscriptionTargets() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/statmod.mixins.json"))).getAsJsonObject();

        assertTrue(root.get("required").getAsBoolean());
        assertEquals(Set.of("IronInscriptionTableMenuMixin"),
                strings(root, "mixins"));
        assertEquals(Set.of("IronInscriptionTableScreenMixin"),
                strings(root, "client"));
    }

    private static Set<String> strings(JsonObject root, String key) {
        Set<String> values = new HashSet<>();
        root.getAsJsonArray(key).forEach(value -> values.add(value.getAsString()));
        return values;
    }
}
