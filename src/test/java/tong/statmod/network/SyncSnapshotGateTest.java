package tong.statmod.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncSnapshotGateTest {
    @Test
    void statsSnapshotsOnlySendOnFirstOrChangedState() {
        SyncSnapshotGate gate = new SyncSnapshotGate();
        UUID playerId = UUID.randomUUID();

        assertTrue(gate.shouldSendStats(playerId, new int[]{1, 2}, new int[]{3, 4}, 5, 6, 7));
        assertFalse(gate.shouldSendStats(playerId, new int[]{1, 2}, new int[]{3, 4}, 5, 6, 7));
        assertTrue(gate.shouldSendStats(playerId, new int[]{1, 9}, new int[]{3, 4}, 5, 6, 7));
    }

    @Test
    void snapshotsAreDefensivelyCopiedBeforeCaching() {
        SyncSnapshotGate gate = new SyncSnapshotGate();
        UUID playerId = UUID.randomUUID();
        int[] levels = {1, 2};
        int[] xp = {3, 4};

        assertTrue(gate.shouldSendStats(playerId, levels, xp, 5, 6, 7));

        levels[0] = 99;
        xp[0] = 88;

        assertFalse(gate.shouldSendStats(playerId, new int[]{1, 2}, new int[]{3, 4}, 5, 6, 7));
    }

    @Test
    void clearingAPlayerForcesTheNextSnapshotToBeSentAgain() {
        SyncSnapshotGate gate = new SyncSnapshotGate();
        UUID playerId = UUID.randomUUID();

        assertTrue(gate.shouldSendPerks(playerId, new int[]{10, 11}, new int[]{2, 3}));
        assertFalse(gate.shouldSendPerks(playerId, new int[]{10, 11}, new int[]{2, 3}));

        assertTrue(gate.shouldSendStamina(playerId, 40.0f, 3.5f, true));
        assertFalse(gate.shouldSendStamina(playerId, 40.0f, 3.5f, true));

        assertTrue(gate.shouldSendMagic(playerId,
                new String[]{"node/a"}, new String[]{"spell/a"}, 12, new int[]{1, 2}, 3, 4));
        assertFalse(gate.shouldSendMagic(playerId,
                new String[]{"node/a"}, new String[]{"spell/a"}, 12, new int[]{1, 2}, 3, 4));

        gate.clear(playerId);

        assertTrue(gate.shouldSendPerks(playerId, new int[]{10, 11}, new int[]{2, 3}));
        assertTrue(gate.shouldSendStamina(playerId, 40.0f, 3.5f, true));
        assertTrue(gate.shouldSendMagic(playerId,
                new String[]{"node/a"}, new String[]{"spell/a"}, 12, new int[]{1, 2}, 3, 4));
    }
}
