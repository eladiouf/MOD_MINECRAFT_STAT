package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TensuraSkillIdsTest {
    @Test
    void canonicalizesLegacyOrMistypedSkillIds() {
        assertEquals("tensura:thought_acceleration", TensuraSkillIds.canonicalize("tensura:accelerated_thoughts"));
        assertEquals("tensura:spatial_manipulation", TensuraSkillIds.canonicalize("tensura:space_manipulation"));
        assertEquals("tensura:ultraspeed_regeneration", TensuraSkillIds.canonicalize("tensura:ultra_speed_regeneration"));
        assertEquals("tensura:ultraspeed_regeneration", TensuraSkillIds.canonicalize("tensura:ultra_regeneration"));
        assertEquals("tensura:universal_perception", TensuraSkillIds.canonicalize("tensura:universal_detect"));
        assertEquals("tensura:creator", TensuraSkillIds.canonicalize("tensura:creation"));
        assertEquals("tensura:cook", TensuraSkillIds.canonicalize("tensura:cooking"));
    }
}
