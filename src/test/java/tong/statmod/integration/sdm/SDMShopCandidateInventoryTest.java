package tong.statmod.integration.sdm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SDMShopCandidateInventoryTest {
    @Test
    void candidateInventoryHasRequiredModsAndNoForbiddenIds() throws IOException {
        Path path = Path.of("docs/generated/sdm-shop-item-candidates.tsv");
        assertTrue(Files.exists(path));
        String tsv = Files.readString(path);
        for (String mod : List.of("minecraft", "statmod", "tensura",
                "irons_spellbooks", "iceandfire", "apotheosis", "epicfight",
                "simplyswords", "magistuarmory", "overgeared")) {
            assertTrue(tsv.contains(mod + "\t"), mod);
        }
        assertFalse(tsv.contains("spawn_egg"));
        assertFalse(tsv.contains("boss_summoner"));
    }
}
