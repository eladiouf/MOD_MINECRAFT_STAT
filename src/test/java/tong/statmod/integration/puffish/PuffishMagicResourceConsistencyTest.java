package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishMagicResourceConsistencyTest {
    @Test
    void unified_magic_category_is_declared_and_backed_by_resources() throws Exception {
        ClassLoader loader = PuffishMagicResourceConsistencyTest.class.getClassLoader();

        String configJson;
        try (InputStream stream = loader.getResourceAsStream("data/statmod/puffish_skills/config.json")) {
            assertNotNull(stream, "missing puffish config");
            configJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        for (MagicNode node : MagicTreeCatalog.all()) {
            String categoryId = PuffishMagicCategoryIds.categoryFor(node.id());
            assertNotNull(categoryId, "node must map to a Puffish category: " + node.id());
            assertTrue("statmod:statmod_magic".equals(categoryId),
                    "node must resolve to the unified magic category: " + node.id());
        }

        String category = "statmod_magic";
        assertTrue(configJson.contains('"' + category + '"'),
                "config must declare category " + category);
        assertHasResource(loader, category, "category.json");
        assertHasResource(loader, category, "definitions.json");
        assertHasResource(loader, category, "skills.json");
        assertHasResource(loader, category, "connections.json");

        try (InputStream stream = loader.getResourceAsStream(
                "data/statmod/puffish_skills/categories/statmod_magic/definitions.json")) {
            assertNotNull(stream, "missing unified magic definitions");
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"common.foundation.arcane_focus\""));
            assertTrue(json.contains("\"fire.signature.fireball\""));
            assertTrue(json.contains("\"water.signature.blizzard\""));
            assertTrue(json.contains("\"blood.signature.sacrifice\""));
            assertTrue(json.contains("\"eldritch.signature.pocket_dimension\""));
        }

        try (InputStream stream = loader.getResourceAsStream(
                "data/statmod/puffish_skills/categories/statmod_magic_common/category.json")) {
            assertTrue(stream == null, "legacy split magic categories should be removed");
        }
    }

    private static void assertHasResource(ClassLoader loader, String category, String fileName) {
        String path = "data/statmod/puffish_skills/categories/" + category + "/" + fileName;
        try (InputStream stream = loader.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
        } catch (Exception e) {
            throw new AssertionError("failed to read resource " + path, e);
        }
    }
}
