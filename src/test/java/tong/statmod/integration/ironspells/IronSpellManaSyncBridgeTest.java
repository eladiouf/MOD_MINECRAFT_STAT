package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSpellManaSyncBridgeTest {
    @Test
    void forcedClientSyncStillBroadcastsWhenServerManaAlreadyMatches() {
        assertTrue(IronSpellManaSyncBridge.shouldBroadcastClientSync(320.0f, 320.0f, true));
    }

    @Test
    void ordinaryManaUpdatesSkipRedundantPacketsWhenServerManaAlreadyMatches() {
        assertFalse(IronSpellManaSyncBridge.shouldBroadcastClientSync(320.0f, 320.0f, false));
    }
}
