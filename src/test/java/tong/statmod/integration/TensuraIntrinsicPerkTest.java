package tong.statmod.integration;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.TensuraEventSubscriber;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TensuraIntrinsicPerkTest {

    @Test
    void mapsIntrinsicSkillsToFreePerks() {
        assertArrayEquals(new int[] {0, 1}, TensuraEventSubscriber.intrinsicPerkIdsForSkill("tensura:ogre_berserker"));
        assertArrayEquals(new int[] {24}, TensuraEventSubscriber.intrinsicPerkIdsForSkill("tensura:dragon_skin"));
        assertArrayEquals(new int[] {48, 36}, TensuraEventSubscriber.intrinsicPerkIdsForSkill("tensura:dragon_eye"));
    }

    @Test
    void returnsNullForUnknownIntrinsicSkill() {
        assertNull(TensuraEventSubscriber.intrinsicPerkIdsForSkill("tensura:unknown_skill"));
    }
}
