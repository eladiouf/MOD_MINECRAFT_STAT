package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishMagicVisualAtlasLayoutTest {
    @Test
    void debug_layout_snapshot_exposes_root_and_bounds() {
        PuffishMagicTreeBuilder.LayoutSnapshot snapshot = PuffishMagicTreeBuilder.debugLayoutSnapshot();
        assertNotNull(snapshot);
        assertNotNull(snapshot.root());
        assertNotNull(snapshot.bounds());
        assertEquals("common/foundation/arcane_focus",
                snapshot.placements().entrySet().stream()
                        .filter(entry -> entry.getValue().equals(snapshot.root()))
                        .findFirst()
                        .orElseThrow()
                        .getKey());
    }

    @Test
    void root_is_close_to_visual_center_after_recentering() {
        PuffishMagicTreeBuilder.LayoutSnapshot snapshot = PuffishMagicTreeBuilder.debugLayoutSnapshot();
        int dx = Math.abs(snapshot.root().x() - snapshot.boundsCenter().x());
        int dy = Math.abs(snapshot.root().y() - snapshot.boundsCenter().y());
        assertTrue(dx <= 40, "root should be visually centered on X, actual delta=" + dx);
        assertTrue(dy <= 60, "root should be visually centered on Y, actual delta=" + dy);
    }

    @Test
    void root_keeps_a_larger_halo_than_standard_spell_nodes() {
        PuffishMagicTreeBuilder.LayoutSnapshot snapshot = PuffishMagicTreeBuilder.debugLayoutSnapshot();
        double rootNearest = PuffishMagicTreeBuilder.nearestDistanceFrom(
                "common/foundation/arcane_focus", snapshot.placements());
        double fireNearest = PuffishMagicTreeBuilder.nearestDistanceFrom(
                "fire/signature/fireball", snapshot.placements());
        assertTrue(rootNearest >= fireNearest + 60.0,
                "root should keep a stronger breathing halo than ordinary signature nodes"
                        + " (root=" + rootNearest + ", fire=" + fireNearest + ")");
    }
}
