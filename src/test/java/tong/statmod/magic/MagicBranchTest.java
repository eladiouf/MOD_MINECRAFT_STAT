package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagicBranchTest {
    @Test
    void elemental_branches_have_stable_ordinals() {
        assertEquals("common", MagicBranch.COMMON.id);
        assertEquals("fire", MagicBranch.FIRE.id);
        assertEquals("water", MagicBranch.WATER.id);
        assertEquals("air", MagicBranch.AIR.id);
        assertEquals("earth", MagicBranch.EARTH.id);
    }

    @Test
    void late_game_branches_present() {
        assertTrue(MagicBranch.HOLY.lateGame);
        assertTrue(MagicBranch.BLOOD.lateGame);
        assertTrue(MagicBranch.ENDER.lateGame);
        assertTrue(MagicBranch.EVOCATION.lateGame);
        assertTrue(MagicBranch.ELDRITCH.lateGame);
        assertFalse(MagicBranch.FIRE.lateGame);
    }

    @Test
    void resolve_by_id_round_trips() {
        for (MagicBranch b : MagicBranch.values()) {
            assertSame(b, MagicBranch.byId(b.id));
        }
        assertNull(MagicBranch.byId("not_a_branch"));
    }
}
