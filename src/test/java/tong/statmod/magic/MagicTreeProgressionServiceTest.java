package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicTreeProgressionServiceTest {

    /** Sature toutes les stats pour que les 3 gates ne soient jamais le blocker dans ces tests. */
    private static void saturateStats(PlayerStatData d) {
        for (StatType s : StatType.values()) {
            d.setLevel(s.index, 20);
        }
    }

    @Test
    void unlock_records_node_spends_points_and_learns_spells() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        saturateStats(d);
        d.addArcanePoints(5);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, opener);

        assertTrue(r.success(), "expected unlock to succeed, got failure: " + r.failure());
        assertTrue(d.hasMagicNode("fire/opener/ignition"));
        // Sous la monnaie unifiée, le pool combiné a perdu opener.cost() (=2).
        assertEquals(3, d.getMagicPoints());
    }

    @Test
    void unlock_school_node_spends_unified_points() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        saturateStats(d);
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.addMagicNode("fire/opener/ignition");
        MagicNode ember = MagicTreeCatalog.byId("fire/tier/ember_path");

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, ember);

        assertTrue(r.success(), "expected unlock to succeed, got failure: " + r.failure());
        // ember.cost() = 1, started avec 3 dans le pool unifié
        assertEquals(2, d.getMagicPoints());
    }

    @Test
    void unlock_signature_node_learns_referenced_spells() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        saturateStats(d);
        d.addSchoolPoints(MagicBranch.FIRE, 5);
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/tier/ember_path");
        MagicNode firebolt = MagicTreeCatalog.byId("fire/signature/firebolt");

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, firebolt);
        assertTrue(r.success(), "expected unlock to succeed, got failure: " + r.failure());
        assertTrue(d.hasLearnedSpell("irons_spellbooks:firebolt"));
    }

    @Test
    void unlock_can_grant_tensura_runtime_rewards_for_new_skills() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        saturateStats(d);
        d.addSchoolPoints(MagicBranch.FIRE, 5);
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/tier/ember_path");
        java.util.List<String> granted = new java.util.ArrayList<>();
        MagicNode node = new MagicNode(
                "fire/signature/test_tensura_fire",
                MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL,
                MagicTier.T1,
                MagicCurrency.SCHOOL,
                1,
                java.util.List.of("fire/tier/ember_path"),
                java.util.Set.of("tensura:fire_bolt"),
                SpellRole.ELEMENTAL_DAMAGE_FIRE);

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, node, granted::add);
        assertTrue(r.success(), "expected unlock to succeed, got failure: " + r.failure());
        assertTrue(d.hasLearnedSpell("tensura:fire_bolt"));
        assertTrue(d.hasLearnedSpell("statmod:tensura_fire_bolt"));
        assertEquals(java.util.List.of("tensura:fire_bolt"), granted);
    }

    @Test
    void unlock_rolls_back_when_tensura_runtime_grant_fails() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        saturateStats(d);
        d.addSchoolPoints(MagicBranch.FIRE, 5);
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/tier/ember_path");
        MagicNode node = new MagicNode(
                "fire/signature/test_failed_tensura_fire",
                MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL,
                MagicTier.T1,
                MagicCurrency.SCHOOL,
                1,
                java.util.List.of("fire/tier/ember_path"),
                java.util.Set.of("tensura:fire_bolt"),
                SpellRole.ELEMENTAL_DAMAGE_FIRE);

        MagicTreeProgressionService.UnlockResult result = MagicTreeProgressionService.tryUnlock(d, node, skillId -> false);

        assertFalse(result.success());
        assertEquals(MagicEligibilityResolver.Failure.RUNTIME_GRANT_FAILED, result.failure());
        // Rollback restitue les points unifiés
        assertEquals(5, d.getMagicPoints());
        assertFalse(d.hasMagicNode(node.id()));
        assertFalse(d.hasLearnedSpell("tensura:fire_bolt"));
        assertFalse(d.hasLearnedSpell("statmod:tensura_fire_bolt"));
    }

    @Test
    void unlock_failure_returns_typed_reason() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        saturateStats(d);
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, opener);
        assertFalse(r.success());
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ, r.failure());
    }

    @Test
    void lategame_opener_refuses_when_multi_school_gate_missing() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.HUMAN);
        saturateStats(d);
        d.addArcanePoints(99);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        d.addMagicNode("common/foundation/cast_discipline");
        MagicNode holyOpener = MagicTreeCatalog.byId("holy/opener/light_awakening");
        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, holyOpener);
        assertFalse(r.success());
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ, r.failure());
    }
}
