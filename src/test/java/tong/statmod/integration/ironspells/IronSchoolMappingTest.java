package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import static org.junit.jupiter.api.Assertions.*;

class IronSchoolMappingTest {
    @Test
    void irons_fire_maps_to_fire_branch() {
        assertEquals(MagicBranch.FIRE,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "fire"));
    }

    @Test
    void irons_ice_maps_to_water_branch() {
        assertEquals(MagicBranch.WATER,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "ice"));
    }

    @Test
    void irons_lightning_maps_to_air_branch() {
        assertEquals(MagicBranch.AIR,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "lightning"));
    }

    @Test
    void irons_nature_maps_to_earth_branch() {
        assertEquals(MagicBranch.EARTH,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "nature"));
    }

    @Test
    void late_game_schools_map_one_to_one() {
        assertEquals(MagicBranch.HOLY, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "holy"));
        assertEquals(MagicBranch.BLOOD, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "blood"));
        assertEquals(MagicBranch.ENDER, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "ender"));
        assertEquals(MagicBranch.EVOCATION, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "evocation"));
        assertEquals(MagicBranch.ELDRITCH, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "eldritch"));
    }

    @Test
    void unknown_school_returns_null() {
        assertNull(IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "unknown_school"));
        assertNull(IronSchoolMapping.fromIronsSchoolId(null, "fire"));
    }
}
