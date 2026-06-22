package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishUnifiedPerkCategoryResourceTest {
    @Test
    void unifiedPerkCategoryFilesExist() throws Exception {
        ClassLoader loader = PuffishUnifiedPerkCategoryResourceTest.class.getClassLoader();
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/category.json");
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/definitions.json");
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/skills.json");
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/connections.json");
    }

    @Test
    void unifiedDefinitionsStillContainMultipleFamilies() throws Exception {
        try (InputStream stream = PuffishUnifiedPerkCategoryResourceTest.class.getClassLoader()
                .getResourceAsStream("data/statmod/puffish_skills/categories/statmod_perks/definitions.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"brute_force__brute_core\""));
            assertTrue(json.contains("\"precision__preci_core\""));
            assertTrue(json.contains("\"arcane_power__arcane_core\""));
            assertTrue(json.contains("\"forging__forge_core\""));
        }
    }

    @Test
    void legacy_split_perk_categoriesAreRemoved() throws Exception {
        try (InputStream stream = PuffishUnifiedPerkCategoryResourceTest.class.getClassLoader()
                .getResourceAsStream("data/statmod/puffish_skills/categories/frontline_physical_combat/category.json")) {
            assertTrue(stream == null, "legacy split perk categories should be removed");
        }
    }

    private static void assertHas(ClassLoader loader, String path) throws Exception {
        try (InputStream stream = loader.getResourceAsStream(path)) {
            assertNotNull(stream, path);
        }
    }
}
