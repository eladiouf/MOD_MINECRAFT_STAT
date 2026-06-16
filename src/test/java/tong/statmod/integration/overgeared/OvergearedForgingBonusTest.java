package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import net.stirdrem.overgeared.ForgingQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvergearedForgingBonusTest {
    @Test
    void delegatesScalingAndThresholdLogic() {
        assertEquals(9, OvergearedForgingBonus.scaledInteractionXp(5, ForgingQuality.MASTER));
        assertEquals(2.0f, OvergearedForgingBonus.breakSpeedMultiplier(50), 0.0001f);
        assertEquals(0, OvergearedForgingBonus.adjustedDurabilityDamage(4, 10));
        assertEquals(35, OvergearedForgingBonus.requiredForgingLevel(ResourceLocation.parse("statmod:perk_tome")));
        assertTrue(OvergearedForgingBonus.canCraftSpecialOutput(ResourceLocation.parse("statmod:perk_tome"), 35));
        assertFalse(OvergearedForgingBonus.canCraftSpecialOutput(ResourceLocation.parse("statmod:perk_tome"), 34));
    }
}
