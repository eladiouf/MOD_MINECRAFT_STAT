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

    @Test
    void root_stays_tightly_centered_in_the_visual_atlas() {
        PuffishMagicTreeBuilder.LayoutSnapshot snapshot = PuffishMagicTreeBuilder.debugLayoutSnapshot();
        int dx = Math.abs(snapshot.root().x() - snapshot.boundsCenter().x());
        int dy = Math.abs(snapshot.root().y() - snapshot.boundsCenter().y());
        assertTrue(dx <= 20, "root should be tightly centered on X, actual delta=" + dx);
        assertTrue(dy <= 40, "root should be tightly centered on Y, actual delta=" + dy);
    }

    @Test
    void air_and_earth_openers_balance_their_diagonal_quadrants() {
        PuffishMagicTreeBuilder.LayoutSnapshot snapshot = PuffishMagicTreeBuilder.debugLayoutSnapshot();
        PuffishMagicTreeBuilder.NodePlacement root = snapshot.root();
        PuffishMagicTreeBuilder.NodePlacement air = snapshot.placements().get("air/opener/spark_awakening");
        PuffishMagicTreeBuilder.NodePlacement earth = snapshot.placements().get("earth/opener/nature_awakening");
        assertNotNull(air);
        assertNotNull(earth);
        int horizontalDelta = Math.abs(Math.abs(root.x() - air.x()) - Math.abs(earth.x() - root.x()));
        int verticalDelta = Math.abs(Math.abs(root.y() - air.y()) - Math.abs(earth.y() - root.y()));
        assertTrue(horizontalDelta <= 60,
                "air and earth should keep comparable horizontal reach, delta=" + horizontalDelta);
        assertTrue(verticalDelta <= 60,
                "air and earth should keep comparable vertical reach, delta=" + verticalDelta);
    }

    @Test
    void air_and_eldritch_openers_keep_clear_breathing_room() {
        PuffishMagicTreeBuilder.LayoutSnapshot snapshot = PuffishMagicTreeBuilder.debugLayoutSnapshot();
        double distance = distanceBetween(snapshot,
                "air/opener/spark_awakening",
                "eldritch/opener/dark_awakening");
        assertTrue(distance >= 96.0,
                "air and eldritch should not visually collapse into the same lane, actual distance=" + distance);
    }

    private static double distanceBetween(PuffishMagicTreeBuilder.LayoutSnapshot snapshot, String aId, String bId) {
        PuffishMagicTreeBuilder.NodePlacement a = snapshot.placements().get(aId);
        PuffishMagicTreeBuilder.NodePlacement b = snapshot.placements().get(bId);
        assertNotNull(a, "missing node " + aId);
        assertNotNull(b, "missing node " + bId);
        int dx = a.x() - b.x();
        int dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
