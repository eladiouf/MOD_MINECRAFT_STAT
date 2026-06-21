package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class MagicEligibilityResolverTest {
    private PlayerStatData fresh() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.ELF);
        return d;
    }

    @Test
    void missing_prereq_fails() {
        PlayerStatData d = fresh();
        d.addArcanePoints(99);
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ,
                MagicEligibilityResolver.evaluate(d, opener).failure());
    }

    @Test
    void not_enough_arcane_fails() {
        PlayerStatData d = fresh();
        MagicNode root = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicEligibilityResolver.Failure.NOT_ENOUGH_POINTS,
                MagicEligibilityResolver.evaluate(d, root).failure());
    }

    @Test
    void ok_when_prereqs_met_and_points_enough() {
        PlayerStatData d = fresh();
        d.addArcanePoints(5);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        assertEquals(MagicEligibilityResolver.Failure.NONE,
                MagicEligibilityResolver.evaluate(d, opener).failure());
    }

    @Test
    void natural_affinity_does_not_change_cost_but_out_of_affinity_inflates_school_cost() {
        PlayerStatData d = fresh();
        d.addMagicNode("fire/opener/ignition");
        d.addSchoolPoints(MagicBranch.FIRE, 1);
        MagicNode ember = MagicTreeCatalog.byId("fire/tier/ember_path");
        var eval = MagicEligibilityResolver.evaluate(d, ember);
        assertEquals(2, eval.adjustedCost());
        assertEquals(MagicEligibilityResolver.Failure.NOT_ENOUGH_POINTS, eval.failure());
    }

    @Test
    void natural_affinity_keeps_base_cost_for_dwarf_fire() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addMagicNode("fire/opener/ignition");
        d.addSchoolPoints(MagicBranch.FIRE, 1);
        MagicNode ember = MagicTreeCatalog.byId("fire/tier/ember_path");
        var eval = MagicEligibilityResolver.evaluate(d, ember);
        assertEquals(1, eval.adjustedCost());
        assertEquals(MagicEligibilityResolver.Failure.NONE, eval.failure());
    }

    @Test
    void second_natural_branch_cheap_after_first_taken() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.ELF);
        d.setChosenStartBranch(MagicBranch.AIR);
        int adjustedWater = MagicEligibilityResolver.affinityAdjustedCost(d, MagicBranch.WATER, 4);
        int adjustedFire = MagicEligibilityResolver.affinityAdjustedCost(d, MagicBranch.FIRE, 4);
        assertTrue(adjustedWater < adjustedFire, "second natural branch must be cheaper than out-of-affinity");
    }

    @Test
    void locked_sentinel_prereq_is_unsatisfiable() {
        PlayerStatData d = fresh();
        d.addArcanePoints(99);
        MagicNode lockedAnchor = MagicTreeCatalog.byId("water/locked/anchor");
        assertEquals(MagicEligibilityResolver.Failure.LOCKED,
                MagicEligibilityResolver.evaluate(d, lockedAnchor).failure());
    }
}
