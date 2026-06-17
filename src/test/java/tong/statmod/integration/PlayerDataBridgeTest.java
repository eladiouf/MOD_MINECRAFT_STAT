package tong.statmod.integration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataBridgeTest {
    @Test
    void skillLookupAcceptsLearnedAndIntrinsicSkillIds() {
        assertTrue(PlayerDataBridge.hasSkillId(
                List.of("tensura:parallel_existence"),
                List.of("tensura:ogre_berserker"),
                "tensura:parallel_existence"
        ));

        assertTrue(PlayerDataBridge.hasSkillId(
                List.of(),
                List.of("tensura:dragon_eye"),
                "tensura:dragon_eye"
        ));

        assertFalse(PlayerDataBridge.hasSkillId(
                List.of("tensura:parallel_existence"),
                List.of("tensura:dragon_eye"),
                "tensura:thought_acceleration"
        ));
    }
}
