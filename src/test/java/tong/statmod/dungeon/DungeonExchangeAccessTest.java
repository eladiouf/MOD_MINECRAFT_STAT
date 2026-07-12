package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonExchangeAccessTest {

    @Test
    void allowsRecentInteractionNearTaggedExchangerInSameDimension() {
        assertTrue(DungeonExchangeAccess.isAllowed(100, 200, true, true, 16.0));
    }

    @Test
    void rejectsExpiredOrRemoteOrForgedConversion() {
        assertFalse(DungeonExchangeAccess.isAllowed(201, 200, true, true, 16.0));
        assertFalse(DungeonExchangeAccess.isAllowed(100, 200, false, true, 16.0));
        assertFalse(DungeonExchangeAccess.isAllowed(100, 200, true, false, 16.0));
        assertFalse(DungeonExchangeAccess.isAllowed(100, 200, true, true, 64.01));
    }
}
