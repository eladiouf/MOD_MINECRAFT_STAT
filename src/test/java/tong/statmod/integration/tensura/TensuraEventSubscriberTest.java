package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.TensuraEventSubscriber;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraEventSubscriberTest {

    @Test
    void recognizesUltimateSkillIdentifiers() {
        assertTrue(TensuraEventSubscriber.isUltimateSkillId("tensura:ultimate_skill"));
        assertTrue(TensuraEventSubscriber.isUltimateSkillId("tensura:transcendence"));
    }
}
