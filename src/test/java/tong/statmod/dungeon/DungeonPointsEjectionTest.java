package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DungeonPointsEjectionTest {

    @Test
    void ejectsWhenInDungeonAndZeroPoints() {
        assertTrue(DungeonPointsEjection.shouldEject(true, 0));
    }

    @Test
    void doesNotEjectWithPositivePoints() {
        assertFalse(DungeonPointsEjection.shouldEject(true, 5));
    }

    @Test
    void doesNotEjectOutsideDungeon() {
        assertFalse(DungeonPointsEjection.shouldEject(false, 0));
    }
}
