package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronSpellAttributeBridgeTest {
    private static final float EPSILON = 0.0001f;

    @Test
    void loginRestoreFillsToMaxWhenNoPersistedSnapshotExists() {
        assertEquals(1250.0f, IronSpellAttributeBridge.resolveRestoredMana(-1.0f, 1250.0d), EPSILON);
    }

    @Test
    void loginRestoreUsesPersistedSnapshotInsteadOfLiveZeroMana() {
        assertEquals(320.0f, IronSpellAttributeBridge.resolveRestoredMana(320.0f, 1250.0d), EPSILON);
    }

    @Test
    void loginRestoreClampsPersistedSnapshotToTheNewMaximum() {
        assertEquals(900.0f, IronSpellAttributeBridge.resolveRestoredMana(1200.0f, 900.0d), EPSILON);
    }
}
