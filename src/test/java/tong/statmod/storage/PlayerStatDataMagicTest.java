package tong.statmod.storage;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatDataMagicTest {
    @Test
    void arcane_points_default_zero_and_increment() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(0, d.getArcanePoints());
        d.addArcanePoints(5);
        assertEquals(5, d.getArcanePoints());
        d.addArcanePoints(-3);
        assertEquals(2, d.getArcanePoints());
        d.addArcanePoints(-99);
        assertEquals(0, d.getArcanePoints());
    }

    @Test
    void school_points_per_branch_isolated() {
        PlayerStatData d = new PlayerStatData();
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.addSchoolPoints(MagicBranch.WATER, 1);
        assertEquals(3, d.getSchoolPoints(MagicBranch.FIRE));
        assertEquals(1, d.getSchoolPoints(MagicBranch.WATER));
        assertEquals(0, d.getSchoolPoints(MagicBranch.AIR));
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
    void school_mastery_progress_accumulates_to_threshold() {
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
