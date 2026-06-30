package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests pour le resolver refondu sous la nouvelle économie : monnaie unique
 * {@code magicPoints} + 3 gates stats (ARCANE_POWER + ERUDITION + tertiaire) avec deltas race.
 *
 * <p>L'ancien comportement {@code affinityAdjustedCost} (race modifie le coût en points) est
 * supprimé — la race agit sur les seuils de stats désormais. Voir
 * {@code MagicNodeStatRequirementsTest} pour les détails race.
 */
class MagicEligibilityResolverTest {

    private static PlayerStatData fullyEquipped(MagicRace race, MagicBranch chosenStart) {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(race);
        d.setChosenStartBranch(chosenStart);
        d.addArcanePoints(99); // pool unifié = arcane + school sous transition
        // Sature les stats pour que les 3 gates passent.
        d.setLevel(StatType.ARCANE_POWER.index, 20);
        d.setLevel(StatType.ERUDITION.index, 20);
        for (StatType s : StatType.values()) {
            d.setLevel(s.index, Math.max(d.getLevel(s.index), 20));
        }
        return d;
    }

    // ---------- Failures legacy ----------

    @Test
    void missing_prereq_fails() {
        PlayerStatData d = fullyEquipped(MagicRace.ELF, MagicBranch.AIR);
        MagicNode fireOpener = MagicTreeCatalog.byId("fire/opener/ignition");
        // arcane_focus + mana_well sont prereqs — non débloqués → MISSING_PREREQ
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ,
                MagicEligibilityResolver.evaluate(d, fireOpener).failure());
    }

    @Test
    void not_enough_points_fails() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.ELF);
        // 0 magic points + saturated stats → bloqué par cost
        d.setLevel(StatType.ARCANE_POWER.index, 20);
        d.setLevel(StatType.ERUDITION.index, 20);
        MagicNode root = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicEligibilityResolver.Failure.NOT_ENOUGH_POINTS,
                MagicEligibilityResolver.evaluate(d, root).failure());
    }

    @Test
    void common_trunk_allowed_without_race() {
        PlayerStatData d = new PlayerStatData();
        d.addArcanePoints(5);
        d.setLevel(StatType.ARCANE_POWER.index, 5);
        d.setLevel(StatType.ERUDITION.index, 5);
        MagicNode node = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicEligibilityResolver.Failure.NONE,
                MagicEligibilityResolver.evaluate(d, node).failure());
    }

    @Test
    void non_common_node_fails_with_no_race_selected() {
        PlayerStatData d = new PlayerStatData();
        d.addArcanePoints(99);
        d.setLevel(StatType.ARCANE_POWER.index, 20);
        d.setLevel(StatType.ERUDITION.index, 20);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode node = MagicTreeCatalog.byId("fire/opener/ignition");
        assertEquals(MagicEligibilityResolver.Failure.NO_RACE,
                MagicEligibilityResolver.evaluate(d, node).failure());
    }

    // ---------- 3 gates stats ----------

    @Test
    void low_arcane_power_blocks_with_STAT_REQUIREMENT_NOT_MET() {
        PlayerStatData d = fullyEquipped(MagicRace.ELF, MagicBranch.FIRE);
        d.setLevel(StatType.ARCANE_POWER.index, 0); // sous le seuil
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        MagicEligibilityResolver.Result result = MagicEligibilityResolver.evaluate(d, opener);
        assertEquals(MagicEligibilityResolver.Failure.STAT_REQUIREMENT_NOT_MET, result.failure());
        assertFalse(result.missingStats().isEmpty(), "missing stats should list ARCANE_POWER");
        assertTrue(result.missingStats().stream()
                .anyMatch(g -> g.contains("Arcane Power")));
    }

    @Test
    void low_tertiary_blocks_for_mobility_spell() {
        // Start branch != Water pour que le delta -1 ne ramène pas AGILITY à 0.
        PlayerStatData d = fullyEquipped(MagicRace.HUMAN, MagicBranch.FIRE);
        d.setLevel(StatType.AGILITY.index, 0); // sort de mobilité → AGILITY ≥ 1 required
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        d.addMagicNode("water/opener/ice_awakening");
        d.addMagicNode("water/tier/frost_path");
        MagicNode frostStep = MagicTreeCatalog.byId("water/signature/frost_step");
        MagicEligibilityResolver.Result result = MagicEligibilityResolver.evaluate(d, frostStep);
        assertEquals(MagicEligibilityResolver.Failure.STAT_REQUIREMENT_NOT_MET, result.failure());
        assertTrue(result.missingStats().stream()
                .anyMatch(g -> g.contains("Agility")));
    }

    @Test
    void all_gates_met_returns_NONE() {
        PlayerStatData d = fullyEquipped(MagicRace.HUMAN, MagicBranch.FIRE);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        MagicEligibilityResolver.Result result = MagicEligibilityResolver.evaluate(d, opener);
        assertEquals(MagicEligibilityResolver.Failure.NONE, result.failure());
        assertTrue(result.missingStats().isEmpty());
    }

    // ---------- Race deltas on universal gates ----------

    @Test
    void human_passes_T1_with_minimum_stats() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.HUMAN);
        d.addArcanePoints(5);
        d.setLevel(StatType.ARCANE_POWER.index, 1); // condition.minLevel = 1
        d.setLevel(StatType.ERUDITION.index, 1);    // condition.minLevel = 1
        MagicNode trunk = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicEligibilityResolver.Failure.NONE,
                MagicEligibilityResolver.evaluate(d, trunk).failure());
    }

    // ---------- Compat shim ----------

    @Test
    void deprecated_affinity_adjusted_cost_returns_base() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.BEAST);
        int cost = MagicEligibilityResolver.affinityAdjustedCost(d, MagicBranch.FIRE, 4);
        assertEquals(4, cost,
                "Under unified economy, race no longer modifies cost — returns base");
    }
}
