package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class MagicTreeProgressionServiceTest {
    @Test
    void unlock_records_node_spends_arcane_and_learns_spells() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addArcanePoints(5);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, opener);

        assertTrue(r.success());
        assertTrue(d.hasMagicNode("fire/opener/ignition"));
        assertEquals(3, d.getArcanePoints());
    }

    @Test
    void unlock_spends_school_currency_for_school_nodes() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.addMagicNode("fire/opener/ignition");
        MagicNode ember = MagicTreeCatalog.byId("fire/tier/ember_path");

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, ember);

        assertTrue(r.success());
        assertEquals(2, d.getSchoolPoints(MagicBranch.FIRE));
    }

    @Test
    void unlock_signature_node_learns_referenced_spells() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addSchoolPoints(MagicBranch.FIRE, 5);
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/tier/ember_path");
        MagicNode firebolt = MagicTreeCatalog.byId("fire/signature/firebolt");

        assertTrue(MagicTreeProgressionService.tryUnlock(d, firebolt).success());
        assertTrue(d.hasLearnedSpell("irons_spellbooks:firebolt"));
    }

    @Test
    void unlock_can_grant_tensura_runtime_rewards_for_new_skills() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
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
                java.util.Set.of("tensura:fire_bolt"));

        assertTrue(MagicTreeProgressionService.tryUnlock(d, node, granted::add).success());
        assertTrue(d.hasLearnedSpell("tensura:fire_bolt"));
        assertEquals(java.util.List.of("tensura:fire_bolt"), granted);
    }

    @Test
    void unlock_failure_returns_typed_reason() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, opener);
        assertFalse(r.success());
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ, r.failure());
    }

    @Test
    void unlock_locked_anchor_refuses_even_when_currency_available() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.HUMAN);
        d.addArcanePoints(99);
        MagicNode locked = MagicTreeCatalog.byId("blood/locked/anchor");
        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, locked);
        assertFalse(r.success());
        assertEquals(MagicEligibilityResolver.Failure.LOCKED, r.failure());
    }
}
