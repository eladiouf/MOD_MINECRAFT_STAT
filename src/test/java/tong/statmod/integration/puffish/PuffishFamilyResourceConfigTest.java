package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishFamilyResourceConfigTest {
    @Test
    void checkedInConfigUsesOneUnifiedPerkAndMagicCategory() throws Exception {
        try (InputStream stream = PuffishFamilyResourceConfigTest.class.getClassLoader()
                .getResourceAsStream("data/statmod/puffish_skills/config.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"statmod_perks\""));
            assertTrue(json.contains("\"statmod_magic\""));
            assertFalse(json.contains("\"statmod_magic_common\""));
            assertFalse(json.contains("\"statmod_magic_fire\""));
        }
    }
}
