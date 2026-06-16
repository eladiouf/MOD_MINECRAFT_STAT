package tong.statmod.loot;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootModifierResourceTest {
    @Test
    void lootModifierJsonFilesUseNeoforgePluralDirectory() {
        ClassLoader loader = LootModifierResourceTest.class.getClassLoader();

        try (InputStream chest = loader.getResourceAsStream("data/statmod/loot_modifiers/chest_loot.json");
             InputStream overgeared = loader.getResourceAsStream("data/statmod/loot_modifiers/overgeared_bonus_loot.json")) {
            assertNotNull(chest);
            assertNotNull(overgeared);

            String chestJson = new String(chest.readAllBytes(), StandardCharsets.UTF_8);
            String overgearedJson = new String(overgeared.readAllBytes(), StandardCharsets.UTF_8);

            assertFalse(chestJson.contains("\"condition\": \"minecraft:alternative\""));
            assertFalse(overgearedJson.contains("\"condition\": \"minecraft:alternative\""));
            assertTrue(chestJson.contains("\"condition\": \"minecraft:any_of\""));
            assertTrue(overgearedJson.contains("\"condition\": \"minecraft:any_of\""));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
