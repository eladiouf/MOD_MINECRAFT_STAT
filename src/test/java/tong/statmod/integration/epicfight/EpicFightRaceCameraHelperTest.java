package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpicFightRaceCameraHelperTest {
    @Test
    void dwarf_scale_gets_positive_camera_offset() {
        assertEquals(0.24f, EpicFightRaceCameraHelper.verticalOffset("tensura:dwarf", 0.85f), 0.0001f);
        assertEquals(0.24f, EpicFightRaceCameraHelper.verticalOffset("tensura:divine_dwarf", 0.85f), 0.0001f);
    }

    @Test
    void non_dwarf_races_do_not_shift_camera() {
        assertEquals(0.0f, EpicFightRaceCameraHelper.verticalOffset("tensura:human", 1.0f), 0.0001f);
        assertEquals(0.0f, EpicFightRaceCameraHelper.verticalOffset("tensura:elf", 1.10f), 0.0001f);
        assertEquals(0.0f, EpicFightRaceCameraHelper.verticalOffset("tensura:beastfolk", 1.0f), 0.0001f);
    }

    @Test
    void full_scale_or_larger_dwarves_do_not_shift_camera() {
        assertEquals(0.0f, EpicFightRaceCameraHelper.verticalOffset("tensura:dwarf", 1.0f), 0.0001f);
        assertEquals(0.0f, EpicFightRaceCameraHelper.verticalOffset("tensura:dwarf", 1.15f), 0.0001f);
    }
}
