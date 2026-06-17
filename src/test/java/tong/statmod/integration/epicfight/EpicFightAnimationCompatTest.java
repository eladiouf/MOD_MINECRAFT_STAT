package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightAnimationCompatTest {
    @Test
    void flagsMissingAnimationPayloadsFromExternalEpicFightAddons() {
        assertTrue(EpicFightAnimationCompat.shouldSkipPlayback(null));
        assertFalse(EpicFightAnimationCompat.shouldSkipPlayback(new Object()));
    }
}
