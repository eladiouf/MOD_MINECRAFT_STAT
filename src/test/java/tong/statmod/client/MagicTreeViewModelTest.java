package tong.statmod.client;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.magic.MagicRace;
import static org.junit.jupiter.api.Assertions.*;

class MagicTreeViewModelTest {

    @Test
    void nodeState_unlocked_if_in_cache() {
        ClientMagicCache.update(new String[]{"common/foundation/arcane_focus"}, new String[0], 0, new int[10], -1, -1);
        MagicNode node = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicTreeNodeState.UNLOCKED, MagicTreeViewModel.nodeState(node));
    }

    @Test
    void nodeState_missing_prereq_for_lategame_opener() {
        ClientMagicCache.update(new String[0], new String[0], 99, new int[10], -1, -1);
        MagicNode node = MagicTreeCatalog.byId("holy/opener/light_awakening");
        assertNotNull(node);
        assertEquals(MagicTreeNodeState.MISSING_PREREQ, MagicTreeViewModel.nodeState(node));
    }

    @Test
    void nodeState_missing_prereq() {
        ClientMagicCache.update(new String[0], new String[0], 99, new int[10], -1, -1);
        MagicNode node = MagicTreeCatalog.byId("common/foundation/mana_well");
        assertEquals(MagicTreeNodeState.MISSING_PREREQ, MagicTreeViewModel.nodeState(node));
    }

    @Test
    void nodeState_eligible_with_enough_arcane() {
        ClientMagicCache.update(new String[]{"common/foundation/arcane_focus"}, new String[0], 99, new int[10], -1, -1);
        MagicNode node = MagicTreeCatalog.byId("common/foundation/mana_well");
        assertEquals(MagicTreeNodeState.ELIGIBLE, MagicTreeViewModel.nodeState(node));
    }

    @Test
    void nodeState_missing_points_when_not_enough() {
        ClientMagicCache.update(new String[]{"common/foundation/arcane_focus"}, new String[0], 0, new int[10], -1, -1);
        MagicNode node = MagicTreeCatalog.byId("common/foundation/mana_well");
        assertEquals(MagicTreeNodeState.MISSING_POINTS, MagicTreeViewModel.nodeState(node));
    }

    @Test
    void nodeState_null_returns_locked_sentinel() {
        assertEquals(MagicTreeNodeState.LOCKED_SENTINEL, MagicTreeViewModel.nodeState(null));
    }

    @Test
    void adjustedCost_common_unchanged() {
        ClientMagicCache.update(new String[0], new String[0], 0, new int[10], MagicRace.ELF.ordinal(), -1);
        MagicNode node = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(1, MagicTreeViewModel.adjustedCost(node));
    }

    @Test
    void adjustedCost_no_race_doubles() {
        ClientMagicCache.update(new String[0], new String[0], 0, new int[10], -1, -1);
        MagicNode node = MagicTreeCatalog.byId("fire/tier/ember_path");
        assertEquals(2, MagicTreeViewModel.adjustedCost(node));
    }

    @Test
    void adjustedCost_with_affinity_no_start_unchanged() {
        ClientMagicCache.update(new String[0], new String[0], 0, new int[10], MagicRace.DWARF.ordinal(), -1);
        MagicNode node = MagicTreeCatalog.byId("fire/tier/ember_path");
        assertEquals(1, MagicTreeViewModel.adjustedCost(node));
    }

    @Test
    void adjustedCost_with_affinity_and_same_start_unchanged() {
        ClientMagicCache.update(new String[0], new String[0], 0, new int[10],
                MagicRace.DWARF.ordinal(), MagicBranch.FIRE.ordinal());
        MagicNode node = MagicTreeCatalog.byId("fire/tier/ember_path");
        assertEquals(1, MagicTreeViewModel.adjustedCost(node));
    }

    @Test
    void adjustedCost_with_affinity_diff_start_halved() {
        ClientMagicCache.update(new String[0], new String[0], 0, new int[10],
                MagicRace.DWARF.ordinal(), MagicBranch.EARTH.ordinal());
        MagicNode node = MagicTreeCatalog.byId("fire/tier/ember_path");
        assertEquals(1, MagicTreeViewModel.adjustedCost(node));
    }

    @Test
    void isVisible_hides_node_with_missing_prereq_for_lategame() {
        ClientMagicCache.update(new String[0], new String[0], 0, new int[10], -1, -1);
        assertFalse(MagicTreeViewModel.isVisible(MagicTreeCatalog.byId("holy/opener/light_awakening")));
    }

    @Test
    void isVisible_shows_unlocked() {
        ClientMagicCache.update(new String[]{"common/foundation/arcane_focus"}, new String[0], 0, new int[10], -1, -1);
        assertTrue(MagicTreeViewModel.isVisible(MagicTreeCatalog.byId("common/foundation/arcane_focus")));
    }

    @Test
    void isVisible_shows_node_with_met_prereqs() {
        ClientMagicCache.update(new String[]{"common/foundation/arcane_focus"}, new String[0], 0, new int[10], -1, -1);
        assertTrue(MagicTreeViewModel.isVisible(MagicTreeCatalog.byId("common/foundation/mana_well")));
    }

    @Test
    void isVisible_hides_node_with_missing_prereq() {
        ClientMagicCache.update(new String[0], new String[0], 0, new int[10], -1, -1);
        assertFalse(MagicTreeViewModel.isVisible(MagicTreeCatalog.byId("common/foundation/mana_well")));
    }

    @Test
    void visibleNodes_returns_only_reachable() {
        ClientMagicCache.update(new String[]{"common/foundation/arcane_focus"}, new String[0], 0, new int[10],
                MagicRace.DWARF.ordinal(), MagicBranch.FIRE.ordinal());
        var visible = MagicTreeViewModel.visibleNodes(MagicBranch.COMMON);
        assertTrue(visible.stream().anyMatch(n -> n.id().equals("common/foundation/arcane_focus")));
        assertTrue(visible.stream().anyMatch(n -> n.id().equals("common/foundation/mana_well")));
    }
}
