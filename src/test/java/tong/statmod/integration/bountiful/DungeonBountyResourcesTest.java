package tong.statmod.integration.bountiful;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonBountyResourcesTest {
    private static final Path POOLS = Path.of("src/main/resources/data/bountiful/bounty_pools/statmod");

    private static String read(String file) throws Exception {
        return Files.readString(POOLS.resolve(file));
    }

    @Test
    void slayPoolHasVanillaBaselineAndModdedMobs() throws Exception {
        String s = read("dungeon_slay_objs.json");
        assertTrue(s.contains("\"type\": \"entity\""), "objectifs de type entity");
        assertTrue(s.contains("minecraft:zombie") && s.contains("minecraft:wither_skeleton"),
                "base vanilla garantie");
        assertTrue(s.contains("slu:knight") && s.contains("slu:elite_knight"),
                "mobs de donjon moddés");
    }

    @Test
    void bossPoolHasWardenAndSluBosses() throws Exception {
        String s = read("dungeon_boss_objs.json");
        assertTrue(s.contains("\"type\": \"entity\""));
        assertTrue(s.contains("minecraft:warden"), "boss vanilla garanti");
        assertTrue(s.contains("slu:boss_malenia") && s.contains("slu:boss_artorias"),
                "boss du roster");
    }

    @Test
    void haulPoolAsksForDungeonMaterials() throws Exception {
        String s = read("dungeon_haul_objs.json");
        assertTrue(s.contains("\"type\": \"item\""));
        assertTrue(s.contains("statmod:rune_essence_arcane")
                && s.contains("statmod:rune_essence_pyrium")
                && s.contains("statmod:rune_essence_mithril"), "rune essences");
        assertTrue(s.contains("statmod:respec_stone"), "matériau donjon signature");
    }

    @Test
    void delvePoolTargetsFloorAdvancements() throws Exception {
        String s = read("dungeon_delve_objs.json");
        assertTrue(s.contains("\"type\": \"criteria\""));
        assertTrue(s.contains("statmod:dungeon/delve_10")
                && s.contains("statmod:dungeon/delve_50")
                && s.contains("statmod:dungeon/delve_100"), "cible les advancements de palier");
    }

    @Test
    void delveAdvancementsExistAndAreCodeGranted() throws Exception {
        Path adv = Path.of("src/main/resources/data/statmod/advancement/dungeon");
        for (String f : new String[]{"delve_10.json","delve_25.json","delve_50.json","delve_100.json"}) {
            String s = Files.readString(adv.resolve(f));
            assertTrue(s.contains("\"trigger\": \"minecraft:impossible\""), f + " doit être code-granted");
            assertTrue(s.contains("\"reached\""), f + " doit exposer le critère 'reached'");
        }
    }

    @Test
    void everyDecreeUsesOnlyDungeonPoolsAndFdpRewards() throws Exception {
        Path decrees = Path.of("src/main/resources/data/bountiful/bounty_decrees/bountiful");
        try (var paths = Files.list(decrees)) {
            var files = paths.filter(p -> p.toString().endsWith(".json")).toList();
            assertTrue(files.size() >= 12, "les 12 décrees de métier");
            for (Path file : files) {
                String s = Files.readString(file);
                assertTrue(s.contains("dungeon_slay_objs") && s.contains("dungeon_boss_objs")
                                && s.contains("dungeon_haul_objs") && s.contains("dungeon_delve_objs"),
                        file + " doit lister les 4 pools donjon");
                assertTrue(s.contains("fdp_rewards"), file + " doit récompenser en FDP");
                // Plus aucun pool vanilla partagé.
                assertTrue(!s.contains("_all_objs") && !s.contains("_all_rews")
                                && !s.contains("_equip_rews") && !s.contains("_metal_objs")
                                && !s.contains("_gardening_rews"),
                        file + " ne doit plus référencer de pool vanilla partagé");
                // Ni ses pools de métier vanilla.
                String profession = file.getFileName().toString().replace(".json", "");
                assertTrue(!s.contains(profession + "_objs") && !s.contains(profession + "_rews"),
                        file + " ne doit plus référencer ses pools de métier vanilla");
            }
        }
    }
}
