package tong.statmod.item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Phase δ — vérifie que les 25 shards sont correctement nommés et parsables.
 */
public class RuneShardIdsTest {

    @Test
    void twentyFiveIdsGenerated() {
        List<String> ids = RuneShardIds.allIds();
        assertEquals(25, ids.size());
        assertEquals(25, Set.copyOf(ids).size(), "Les IDs doivent être uniques");
    }

    @Test
    void idFormatIsExpected() {
        assertEquals("common_physical_shard",
                RuneShardIds.id(RuneShardIds.Rarity.COMMON, RuneShardIds.Family.PHYSICAL));
        assertEquals("legendary_spiritual_shard",
                RuneShardIds.id(RuneShardIds.Rarity.LEGENDARY, RuneShardIds.Family.SPIRITUAL));
    }

    @Test
    void rarityParsingIsCorrect() {
        assertEquals(RuneShardIds.Rarity.COMMON, RuneShardIds.rarityOf("common_physical_shard"));
        assertEquals(RuneShardIds.Rarity.LEGENDARY, RuneShardIds.rarityOf("legendary_agile_shard"));
        assertEquals(RuneShardIds.Rarity.EPIC, RuneShardIds.rarityOf("epic_vital_shard"));
    }

    @Test
    void familyParsingIsCorrect() {
        assertEquals(RuneShardIds.Family.PHYSICAL, RuneShardIds.familyOf("common_physical_shard"));
        assertEquals(RuneShardIds.Family.MAGICAL, RuneShardIds.familyOf("rare_magical_shard"));
        assertEquals(RuneShardIds.Family.SPIRITUAL, RuneShardIds.familyOf("legendary_spiritual_shard"));
    }

    @Test
    void invalidIdReturnsNull() {
        assertNull(RuneShardIds.rarityOf("garbage"));
        assertNull(RuneShardIds.familyOf("garbage"));
        assertNull(RuneShardIds.rarityOf(null));
        assertNull(RuneShardIds.familyOf(null));
    }

    @Test
    void allIdsCanBeRoundTripped() {
        // Chaque ID doit être parsable en rarity + family, et re-générer le même ID.
        for (String id : RuneShardIds.allIds()) {
            RuneShardIds.Rarity r = RuneShardIds.rarityOf(id);
            RuneShardIds.Family f = RuneShardIds.familyOf(id);
            assertNotNull(r, "rarity null pour " + id);
            assertNotNull(f, "family null pour " + id);
            assertEquals(id, RuneShardIds.id(r, f));
        }
    }
}
