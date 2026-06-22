package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PuffishMagicCategoryIdsTest {
    @Test
    void node_id_round_trips_through_skill_id() {
        String nodeId = "fire/signature/firebolt";
        String skillId = PuffishMagicCategoryIds.toSkillId(nodeId);
        assertEquals("fire.signature.firebolt", skillId);
        assertEquals(nodeId, PuffishMagicCategoryIds.fromSkillId(skillId));
    }

    @Test
    void every_magic_node_maps_into_the_unified_magic_category() {
        assertEquals("statmod:statmod_magic",
                PuffishMagicCategoryIds.categoryFor("common/foundation/arcane_focus"));
        assertEquals("statmod:statmod_magic",
                PuffishMagicCategoryIds.categoryFor("fire/opener/ignition"));
        assertEquals("statmod:statmod_magic",
                PuffishMagicCategoryIds.categoryFor("blood/opener/sanguine_awakening"));
    }

    @Test
    void unknown_skill_returns_null() {
        assertNull(PuffishMagicCategoryIds.fromSkillId(null));
        assertNull(PuffishMagicCategoryIds.fromSkillId(""));
        assertNull(PuffishMagicCategoryIds.categoryFor("unknown/path"));
    }
}
