package tong.statmod.block;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mission M5 — Phase ε tests.
 *
 * <p>Vérifie les ressources du bloc {@code statmod:enchantment_anvil} et des 8 recettes
 * essence pré-enchantées.
 */
class EnchantmentAnvilResourcesTest {

    private static final Path RESOURCES = Paths.get("src", "main", "resources");
    private static final Path RECIPE_DIR =
            RESOURCES.resolve("data/statmod/recipe/essence");

    @Test
    void enchantmentAnvil_blockstateExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/blockstates/enchantment_anvil.json")));
    }

    @Test
    void enchantmentAnvil_blockModelExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/models/block/enchantment_anvil.json")));
    }

    @Test
    void enchantmentAnvil_itemModelExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/models/item/enchantment_anvil.json")));
    }

    @Test
    void enchantmentAnvil_textureExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/textures/block/enchantment_anvil.png")));
    }

    @Test
    void enchantmentAnvil_lootTableExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("data/statmod/loot_table/blocks/enchantment_anvil.json")));
    }

    @Test
    void enchantmentAnvil_craftingRecipeExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("data/statmod/recipe/enchantment_anvil.json")));
    }

    @Test
    void pickaxeTag_includesEnchantmentAnvil() throws IOException {
        Path tag = RESOURCES.resolve("data/statmod/tags/block/mineable_with_pickaxe.json");
        assertTrue(Files.exists(tag));
        String content = Files.readString(tag);
        assertTrue(content.contains("statmod:enchantment_anvil"),
                "enchantment_anvil should be in mineable_with_pickaxe tag");
        assertTrue(content.contains("statmod:infusion_forge"),
                "infusion_forge should still be in the tag (regression check)");
    }

    @Test
    void essenceRecipes_eightExist() {
        String[] names = {
                "essence_flame_katana",
                "essence_shadow_rapier",
                "essence_wither_claymore",
                "essence_estus_greathammer",
                "essence_magma_greataxe",
                "essence_permafrost_spear",
                "essence_runefused_longsword",
                "essence_netherfused_glaive",
        };
        assertTrue(Files.exists(RECIPE_DIR), "essence recipe dir missing");
        for (String name : names) {
            assertTrue(Files.exists(RECIPE_DIR.resolve(name + ".json")),
                    "missing essence recipe: " + name);
        }
    }

    @Test
    void essenceRecipes_haveEnchantmentComponent() throws IOException {
        String content = Files.readString(RECIPE_DIR.resolve("essence_flame_katana.json"));
        assertTrue(content.contains("minecraft:enchantments"),
                "essence recipe should bake enchantments via components");
        assertTrue(content.contains("minecraft:fire_aspect"),
                "flame_katana should have fire_aspect");
    }

    @Test
    void essenceRecipes_targetSimplySwords() throws IOException {
        // Sanity : les 8 recettes ciblent bien des armes SimplySwords existantes
        String[] targets = {
                "runic_katana", "runic_rapier", "runic_claymore",
                "runic_greathammer", "runic_greataxe", "runic_spear",
                "runic_longsword", "runic_glaive",
        };
        for (String target : targets) {
            boolean found = false;
            for (Path recipe : Files.list(RECIPE_DIR).toList()) {
                String content = Files.readString(recipe);
                if (content.contains("simplyswords:" + target)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "no recipe targeting simplyswords:" + target);
        }
    }
}
