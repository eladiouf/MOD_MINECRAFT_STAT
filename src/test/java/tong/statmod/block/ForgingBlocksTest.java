package tong.statmod.block;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mission M5 — Phase δ tests.
 *
 * <p>Vérifie la présence des ressources clés pour le bloc {@code statmod:infusion_forge}
 * (modèle, blockstate, texture, loot table, tag). On ne touche pas le DeferredRegister
 * directement (incompatible JUnit pur — voir pattern adopté Phase β).
 */
class ForgingBlocksTest {

    private static final Path RESOURCES = Paths.get(
            "src", "main", "resources");

    @Test
    void infusionForge_blockstateExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/blockstates/infusion_forge.json")));
    }

    @Test
    void infusionForge_blockModelExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/models/block/infusion_forge.json")));
    }

    @Test
    void infusionForge_itemModelExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/models/item/infusion_forge.json")));
    }

    @Test
    void infusionForge_textureExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("assets/statmod/textures/block/infusion_forge.png")));
    }

    @Test
    void infusionForge_lootTableExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("data/statmod/loot_table/blocks/infusion_forge.json")));
    }

    @Test
    void infusionForge_pickaxeTagExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("data/statmod/tags/block/mineable_with_pickaxe.json")));
    }

    @Test
    void infusionForge_craftingRecipeExists() {
        assertTrue(Files.exists(
                RESOURCES.resolve("data/statmod/recipe/infusion_forge.json")));
    }

    @Test
    void runeEssence_allThreeTexturesExist() {
        for (String name : new String[]{"rune_essence_arcane", "rune_essence_pyrium", "rune_essence_mithril"}) {
            assertTrue(Files.exists(
                    RESOURCES.resolve("assets/statmod/textures/item/" + name + ".png")),
                    "missing texture: " + name);
            assertTrue(Files.exists(
                    RESOURCES.resolve("assets/statmod/models/item/" + name + ".json")),
                    "missing model: " + name);
            assertTrue(Files.exists(
                    RESOURCES.resolve("data/statmod/recipe/" + name + ".json")),
                    "missing crafting recipe: " + name);
        }
    }

    @Test
    void magicInfusion_fiveRecipesExist() {
        Path dir = RESOURCES.resolve("data/statmod/recipe/infusion");
        assertTrue(Files.exists(dir), "infusion recipes dir missing");
        String[] names = {
                "infusion_runic_rapier",
                "infusion_runic_katana",
                "infusion_runic_claymore",
                "infusion_runic_spear",
                "infusion_brimstone_cutlass",
        };
        for (String name : names) {
            assertTrue(Files.exists(dir.resolve(name + ".json")),
                    "missing infusion recipe: " + name);
        }
    }
}
