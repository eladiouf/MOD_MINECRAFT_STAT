package tong.statmod.storage;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests pour l'état magique du joueur sous la nouvelle économie unifiée (Mission δ).
 *
 * <p>Les anciens setters {@code addArcanePoints} / {@code addSchoolPoints} sont désormais des
 * compat shims qui versent dans le pool unifié {@code magicPoints}. Les anciens champs
 * persistent uniquement pour la deserialization des saves legacy.
 */
class PlayerStatDataMagicTest {

    @Test
    void magic_points_default_zero_and_increment() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(0, d.getMagicPoints());
        d.addMagicPoints(5);
        assertEquals(5, d.getMagicPoints());
        d.addMagicPoints(-3);
        assertEquals(2, d.getMagicPoints());
        d.addMagicPoints(-99);
        assertEquals(0, d.getMagicPoints(), "magicPoints clamps to 0");
    }

    @Test
    void legacy_addArcanePoints_writes_to_unified_pool() {
        PlayerStatData d = new PlayerStatData();
        d.addArcanePoints(5);
        // Compat shim — verse dans magicPoints, pas dans le legacy arcanePoints field.
        assertEquals(5, d.getMagicPoints());
    }

    @Test
    void legacy_addSchoolPoints_writes_to_unified_pool() {
        PlayerStatData d = new PlayerStatData();
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.addSchoolPoints(MagicBranch.WATER, 1);
        // Compat shim — tout va dans le pool unifié, peu importe la branche.
        assertEquals(4, d.getMagicPoints());
    }

    @Test
    void learned_node_unlock_is_idempotent() {
        PlayerStatData d = new PlayerStatData();
        assertFalse(d.hasMagicNode("fire/opener/ignition"));
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/opener/ignition");
        assertTrue(d.hasMagicNode("fire/opener/ignition"));
        assertEquals(1, d.getMagicNodes().length);
    }

    @Test
    void learned_spells_tracked_separately() {
        PlayerStatData d = new PlayerStatData();
        d.learnSpell("irons_spellbooks:firebolt");
        d.learnSpell("irons_spellbooks:firebolt");
        d.learnSpell("irons_spellbooks:fireball");
        assertTrue(d.hasLearnedSpell("irons_spellbooks:firebolt"));
        assertEquals(2, d.getLearnedSpells().length);
    }

    @Test
    void school_mastery_progress_accumulates() {
        PlayerStatData d = new PlayerStatData();
        d.addSchoolMasteryProgress(MagicBranch.FIRE, 40);
        assertEquals(40, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        d.setSchoolMasteryProgress(MagicBranch.FIRE, 0);
        assertEquals(0, d.getSchoolMasteryProgress(MagicBranch.FIRE));
    }

    @Test
    void magic_race_default_unset() {
        PlayerStatData d = new PlayerStatData();
        assertNull(d.getMagicRace());
        d.setMagicRace(MagicRace.ELF);
        assertEquals(MagicRace.ELF, d.getMagicRace());
    }

    @Test
    void chosen_start_branch_defaults_null() {
        PlayerStatData d = new PlayerStatData();
        assertNull(d.getChosenStartBranch());
        d.setChosenStartBranch(MagicBranch.FIRE);
        assertEquals(MagicBranch.FIRE, d.getChosenStartBranch());
    }
}
