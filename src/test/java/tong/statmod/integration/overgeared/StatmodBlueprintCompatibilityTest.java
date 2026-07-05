package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatmodBlueprintCompatibilityTest {
    @Test
    void optionalStatmodRoughRecipesAcceptStatmodBlueprintItems() {
        assertTrue(StatmodBlueprintCompatibility.allowsOptionalForgingBypass(
                ResourceLocation.parse("statmod:blueprint_universal_blade"),
                ResourceLocation.parse("statmod:rough_blade_arcane"),
                false));
    }

    @Test
    void requiredBlueprintRecipesAreNotBypassed() {
        assertFalse(StatmodBlueprintCompatibility.allowsOptionalForgingBypass(
                ResourceLocation.parse("statmod:blueprint_universal_blade"),
                ResourceLocation.parse("statmod:rough_blade_arcane"),
                true));
    }

    @Test
    void nonBlueprintItemsAreRejected() {
        assertFalse(StatmodBlueprintCompatibility.allowsOptionalForgingBypass(
                ResourceLocation.parse("minecraft:paper"),
                ResourceLocation.parse("statmod:rough_blade_arcane"),
                false));
    }

    @Test
    void nonStatmodResultsAreRejected() {
        assertFalse(StatmodBlueprintCompatibility.allowsOptionalForgingBypass(
                ResourceLocation.parse("statmod:blueprint_universal_blade"),
                ResourceLocation.parse("overgeared:iron_sword_blade"),
                false));
    }
}
