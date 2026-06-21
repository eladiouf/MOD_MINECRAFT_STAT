package tong.statmod.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicUnlockFeedbackMessageFactoryTest {
    @Test
    void formatsNotEnoughPointsFeedback() {
        MagicUnlockFeedbackMessageFactory.MagicUnlockFeedbackMessage message =
                MagicUnlockFeedbackMessageFactory.forFailure(
                        MagicTreeCatalog.byId("fire/signature/firebolt"),
                        MagicEligibilityResolver.Failure.NOT_ENOUGH_POINTS);

        assertEquals("Firebolt", message.title().getString());
        assertEquals("Not enough magic points", message.message().getString());
    }

    @Test
    void formatsRuntimeGrantFailureFeedback() {
        MagicUnlockFeedbackMessageFactory.MagicUnlockFeedbackMessage message =
                MagicUnlockFeedbackMessageFactory.forFailure(
                        MagicTreeCatalog.byId("fire/signature/tensura_fire_bolt"),
                        MagicEligibilityResolver.Failure.RUNTIME_GRANT_FAILED);

        assertEquals("Tensura Fire Bolt", message.title().getString());
        assertEquals("Spell unlock failed at runtime", message.message().getString());
    }
}
