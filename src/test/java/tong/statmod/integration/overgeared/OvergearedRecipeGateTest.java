package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvergearedRecipeGateTest {
    @Test
    void identifiesSpecialStatmodForgingOutputs() {
        assertTrue(OvergearedRecipeGate.isForgingRecipeResult(ResourceLocation.parse("statmod:perk_tome")));
        assertTrue(OvergearedRecipeGate.isForgingRecipeResult(ResourceLocation.parse("statmod:respec_stone")));
        assertFalse(OvergearedRecipeGate.isForgingRecipeResult(ResourceLocation.parse("overgeared:steel_plate")));
    }

    @Test
    void appliesForgingThresholdsPerOutput() {
        assertEquals(35, OvergearedRecipeGate.requiredForgingLevel(ResourceLocation.parse("statmod:perk_tome")));
        assertEquals(20, OvergearedRecipeGate.requiredForgingLevel(ResourceLocation.parse("statmod:respec_stone")));
        assertTrue(OvergearedRecipeGate.canCraft(ResourceLocation.parse("statmod:perk_tome"), 35));
        assertFalse(OvergearedRecipeGate.canCraft(ResourceLocation.parse("statmod:perk_tome"), 34));
        assertTrue(OvergearedRecipeGate.canCraft(ResourceLocation.parse("statmod:respec_stone"), 20));
        assertFalse(OvergearedRecipeGate.canCraft(ResourceLocation.parse("statmod:respec_stone"), 19));
    }
}
