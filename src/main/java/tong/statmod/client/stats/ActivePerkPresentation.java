package tong.statmod.client.stats;

import tong.statmod.perks.AutomaticPerkDefinition;

public record ActivePerkPresentation(String id, String nameKey, String descriptionKey) {
    public static ActivePerkPresentation from(AutomaticPerkDefinition definition) {
        String path = definition.id().substring("statmod:".length());
        String key = "perk.statmod." + path;
        return new ActivePerkPresentation(definition.id(), key, key + ".description");
    }
}
