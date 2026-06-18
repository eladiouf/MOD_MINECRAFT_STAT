package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishOverrideResourceTest {
    @Test
    void overridesBrokenDefaultSkillTreesConfig() throws Exception {
        ClassLoader loader = PuffishOverrideResourceTest.class.getClassLoader();

        try (InputStream stream = loader.getResourceAsStream("data/puffish_skills/puffish_skills/config.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"categories\": []"));
        }
    }
}
