package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class StatmodBlueprintCompatibility {
    private static final Set<String> STATMOD_BLUEPRINT_PATHS = Set.of(
            "blueprint_universal_blade",
            "blueprint_universal_pole",
            "blueprint_runic_blade",
            "blueprint_legendary");

    private StatmodBlueprintCompatibility() {}

    public static boolean isStatmodBlueprint(ResourceLocation itemId) {
        return itemId != null
                && "statmod".equals(itemId.getNamespace())
                && STATMOD_BLUEPRINT_PATHS.contains(itemId.getPath());
    }

    public static boolean allowsOptionalForgingBypass(ResourceLocation blueprintId,
                                                      ResourceLocation resultId,
                                                      boolean requiresBlueprint) {
        return !requiresBlueprint
                && isStatmodBlueprint(blueprintId)
                && resultId != null
                && "statmod".equals(resultId.getNamespace())
                && resultId.getPath().startsWith("rough_");
    }
}
