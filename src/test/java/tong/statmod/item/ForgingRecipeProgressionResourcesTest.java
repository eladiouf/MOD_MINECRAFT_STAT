package tong.statmod.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingRecipeProgressionResourcesTest {

    private static final Path RESOURCES = Paths.get("src", "main", "resources");
    private static final Path COMPONENTS = RESOURCES.resolve("data/statmod/recipe/assembly_components");
    private static final Path ASSEMBLY = RESOURCES.resolve("data/statmod/recipe/assembly/simplyswords");

    @Test
    void assemblyComponents_followTieredForgeProgression() throws IOException {
        String woodenGrip = read(COMPONENTS.resolve("wooden_grip.json"));
        String leatherWrap = read(COMPONENTS.resolve("leather_wrap.json"));
        String wireWrap = read(COMPONENTS.resolve("wire_wrap.json"));
        String runicGrip = read(COMPONENTS.resolve("runic_grip.json"));
        String forgeTongs = read(COMPONENTS.resolve("basic_forge_tongs.json"));
        String smithingHammer = read(COMPONENTS.resolve("basic_smithing_hammer.json"));
        String runicBlueprint = read(COMPONENTS.resolve("blueprint_runic_blade.json"));
        String legendaryBlueprint = read(COMPONENTS.resolve("blueprint_legendary.json"));

        assertTrue(woodenGrip.contains("statmod:leather_wrap"));
        assertTrue(woodenGrip.contains("minecraft:stick"));

        assertTrue(leatherWrap.contains("minecraft:leather"));
        assertTrue(leatherWrap.contains("minecraft:string"));

        assertTrue(wireWrap.contains("statmod:leather_wrap"));
        assertTrue(wireWrap.contains("minecraft:iron_nugget"));

        assertTrue(runicGrip.contains("statmod:wooden_grip"));
        assertTrue(runicGrip.contains("statmod:wire_wrap"));
        assertTrue(runicGrip.contains("minecraft:amethyst_shard"));

        assertTrue(forgeTongs.contains("statmod:wire_wrap"));
        assertTrue(smithingHammer.contains("statmod:wire_wrap"));

        assertTrue(runicBlueprint.contains("statmod:blueprint_universal_blade"));
        assertTrue(runicBlueprint.contains("statmod:rune_essence_arcane"));
        assertTrue(legendaryBlueprint.contains("statmod:blueprint_runic_blade"));
    }

    @Test
    void runeEssenceRecipes_useSchoolCatalysts() throws IOException {
        String arcane = read(RESOURCES.resolve("data/statmod/recipe/rune_essence_arcane.json"));
        String pyrium = read(RESOURCES.resolve("data/statmod/recipe/rune_essence_pyrium.json"));
        String mithril = read(RESOURCES.resolve("data/statmod/recipe/rune_essence_mithril.json"));

        assertTrue(arcane.contains("minecraft:ender_pearl"));
        assertTrue(pyrium.contains("minecraft:blaze_powder"));
        assertTrue(pyrium.contains("minecraft:magma_cream"));
        assertTrue(mithril.contains("minecraft:prismarine_crystals"));
        assertTrue(mithril.contains("minecraft:snowball"));
    }

    @Test
    void simplySwordsMagicAssemblies_useRunicGripInsteadOfWoodenGrip() throws IOException {
        for (String recipeName : List.of(
                "runic_halberd",
                "runic_warglaive",
                "runic_cutlass",
                "brimstone_claymore",
                "watcher_claymore")) {
            String content = read(ASSEMBLY.resolve(recipeName + ".json"));
            assertTrue(content.contains("statmod:runic_grip"), recipeName + " should require runic_grip");
            assertFalse(content.contains("statmod:wooden_grip"), recipeName + " should not use wooden_grip");
        }
    }

    @Test
    void assemblyRecipeGenerator_preservesRunicGripForMagicSimplySwordsTiers() throws IOException {
        String source = read(Paths.get("tools", "generate_assembly_recipes.py"));

        assertTrue(source.contains("statmod:runic_grip"));
        assertTrue(source.contains("simplyswords"));
        assertTrue(source.contains("runic_"));
        assertTrue(source.contains("brimstone_"));
    }

    @Test
    void stationManagedWeapons_doNotKeepDirectAssemblyRecipes() {
        for (String recipeName : List.of(
                "runic_rapier",
                "runic_katana",
                "runic_claymore",
                "runic_spear",
                "runic_longsword",
                "runic_glaive",
                "runic_greathammer",
                "runic_greataxe")) {
            assertFalse(Files.exists(ASSEMBLY.resolve(recipeName + ".json")),
                    recipeName + " should be crafted through forge stations, not direct assembly");
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
