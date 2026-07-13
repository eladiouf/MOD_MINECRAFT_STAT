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
}
