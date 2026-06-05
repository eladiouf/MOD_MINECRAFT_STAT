package tong.statmod.client.hud.overlays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalLevelHudLayoutTest {

    @Test
    void overlayStaysAboveSurvivalBars() {
        assertEquals(4, GlobalLevelHudLayout.X);
        assertEquals(4, GlobalLevelHudLayout.Y);
        assertEquals(14, GlobalLevelHudLayout.BAR_Y);
        assertTrue(GlobalLevelHudLayout.BAR_Y + GlobalLevelHudLayout.BAR_HEIGHT < SurvivalOverlay.PANEL_START_Y,
            "global level overlay must stay above the survival HUD block");
    }
}
