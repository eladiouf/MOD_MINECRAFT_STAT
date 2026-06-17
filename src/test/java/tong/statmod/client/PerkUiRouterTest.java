package tong.statmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkUiRouterTest {
    @Test
    void usesPuffishWhenCompatReportsLoaded() {
        assertTrue(PerkUiRouter.shouldUsePuffish(true));
        assertFalse(PerkUiRouter.shouldUsePuffish(false));
    }
}
