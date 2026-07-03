package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase ε — vérifie la stratification EARLY / MID / LATE / ABYSS de la palette par étage.
 */
public class FloorPaletteTest {

    @Test
    void floor1IsEarly() {
        assertEquals(FloorPalette.EARLY, FloorPalette.forFloor(1));
    }

    @Test
    void floor10IsStillEarly() {
        assertEquals(FloorPalette.EARLY, FloorPalette.forFloor(10));
    }

    @Test
    void floor11IsMid() {
        assertEquals(FloorPalette.MID, FloorPalette.forFloor(11));
    }

    @Test
    void floor25IsStillMid() {
        assertEquals(FloorPalette.MID, FloorPalette.forFloor(25));
    }

    @Test
    void floor26IsLate() {
        assertEquals(FloorPalette.LATE, FloorPalette.forFloor(26));
    }

    @Test
    void floor50IsStillLate() {
        assertEquals(FloorPalette.LATE, FloorPalette.forFloor(50));
    }

    @Test
    void floor51IsAbyss() {
        assertEquals(FloorPalette.ABYSS, FloorPalette.forFloor(51));
    }

    @Test
    void floor100IsAbyss() {
        assertEquals(FloorPalette.ABYSS, FloorPalette.forFloor(100));
    }

    @Test
    void classificationCoversAllFloors() {
        // Chaque étage doit avoir une palette bien définie (pas de trou entre EARLY/MID/LATE/ABYSS).
        for (int f = 1; f <= 100; f++) {
            org.junit.jupiter.api.Assertions.assertNotNull(FloorPalette.forFloor(f),
                    "palette null pour floor " + f);
        }
    }
}
