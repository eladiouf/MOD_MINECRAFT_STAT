package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MagicNodeTest {
    @Test
    void node_record_holds_all_fields() {
        Condition cond = new Condition.StatCondition(StatType.ARCANE_POWER, 5);
        MagicNode n = new MagicNode(
                "fire/opener/ignition",
                MagicBranch.FIRE,
                MagicNodeKind.BRANCH_OPENER,
                MagicTier.T1,
                MagicCurrency.ARCANE,
                3,
                List.of("common/foundation/arcane_focus"),
                Set.of("irons_spellbooks:firebolt"),
                cond);
        assertEquals("fire/opener/ignition", n.id());
        assertEquals(MagicBranch.FIRE, n.branch());
        assertEquals(3, n.cost());
        assertEquals(Set.of("irons_spellbooks:firebolt"), n.learnedSpells());
        assertNotNull(n.condition());
    }

    @Test
    void node_condition_can_be_null() {
        MagicNode n = new MagicNode(
                "fire/opener/ignition",
                MagicBranch.FIRE,
                MagicNodeKind.BRANCH_OPENER,
                MagicTier.T1,
                MagicCurrency.ARCANE,
                3,
                List.of("common/foundation/arcane_focus"),
                Set.of("irons_spellbooks:firebolt"),
                null);
        assertNull(n.condition());
    }

    @Test
    void node_id_must_match_branch_prefix() {
        assertThrows(IllegalArgumentException.class, () -> new MagicNode(
                "fire/opener/foo",
                MagicBranch.WATER,
                MagicNodeKind.BRANCH_OPENER,
                MagicTier.T1,
                MagicCurrency.SCHOOL,
                1,
                List.of(),
                Set.of(),
                null));
    }
}
