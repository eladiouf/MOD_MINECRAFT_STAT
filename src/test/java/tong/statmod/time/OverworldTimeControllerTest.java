package tong.statmod.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverworldTimeControllerTest {
    @Test
    void convertsConfiguredPhaseLengthsToVanillaRates() {
        assertEquals(6.25d, OverworldTimeController.dayTicksPerSecond(), 0.0001d);
        assertEquals(12.5d, OverworldTimeController.nightTicksPerSecond(), 0.0001d);
    }
}
