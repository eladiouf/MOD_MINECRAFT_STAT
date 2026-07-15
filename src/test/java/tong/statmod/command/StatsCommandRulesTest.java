package tong.statmod.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatsCommandRulesTest {
    @Test
    void fixesPermissionAndInputBounds() {
        assertEquals(2, StatsCommandRules.ADMIN_PERMISSION);
        assertTrue(StatsCommandRules.validLevel(0));
        assertTrue(StatsCommandRules.validLevel(100));
        assertFalse(StatsCommandRules.validLevel(-1));
        assertFalse(StatsCommandRules.validLevel(101));
        assertTrue(StatsCommandRules.validXpAmount(1));
        assertFalse(StatsCommandRules.validXpAmount(0));
    }
}
