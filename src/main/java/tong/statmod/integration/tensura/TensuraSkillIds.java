package tong.statmod.integration.tensura;

import java.util.Map;

public final class TensuraSkillIds {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("tensura:accelerated_thoughts", "tensura:thought_acceleration"),
            Map.entry("tensura:space_manipulation", "tensura:spatial_manipulation"),
            Map.entry("tensura:ultra_speed_regeneration", "tensura:ultraspeed_regeneration"),
            Map.entry("tensura:ultra_regeneration", "tensura:ultraspeed_regeneration"),
            Map.entry("tensura:universal_detect", "tensura:universal_perception"),
            Map.entry("tensura:creation", "tensura:creator"),
            Map.entry("tensura:cooking", "tensura:cook")
    );

    private TensuraSkillIds() {}

    public static String canonicalize(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return skillId;
        }
        return ALIASES.getOrDefault(skillId, skillId);
    }
}
