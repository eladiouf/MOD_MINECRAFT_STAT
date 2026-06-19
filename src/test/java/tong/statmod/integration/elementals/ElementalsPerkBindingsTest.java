package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementalsPerkBindingsTest {
    @Test
    void baseMasteryUsesExistingElementMasteryPerks() {
        assertEquals(Perk.AIR_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.AIR));
        assertEquals(Perk.WATER_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.WATER));
        assertEquals(Perk.EARTH_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.EARTH));
        assertEquals(Perk.FIRE_MASTERY, ElementalsPerkBindings.masteryPerk(ElementalBranch.FIRE));
    }

    @Test
    void broaderUnlocksUseConcreteExistingPerkIds() {
        assertEquals(Set.of(Perk.ERUDITION_CORE.id, Perk.FIRE_CORE.id),
                ElementalsPerkBindings.thirdUnlockPerks(ElementalBranch.FIRE));
        assertEquals(Set.of(Perk.ERUDITION_MASTERY.id, Perk.WATER_ACTIVE.id),
                ElementalsPerkBindings.fourthUnlockPerks(ElementalBranch.WATER));
    }

    @Test
    void rareRewardsUseFreeGrantedExistingTreePerksWithoutAutoGrantHooks() {
        assertEquals(Perk.AIR_TRANSCENDENCE, ElementalsPerkBindings.rareRewardPerk(ElementalBranch.LIGHTNING));
        assertEquals(Perk.WILL_TRANSCENDENCE, ElementalsPerkBindings.rareRewardPerk(ElementalBranch.BLOOD));
    }
}
