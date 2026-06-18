package tong.statmod.perks;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicPerkCatalogTest {
    @Test
    void everyMagicalStatGetsATierChain() {
        assertEquals(Perk.ARCANE_CORE, Perk.byStatAndTier(StatType.ARCANE_POWER, PerkTier.CORE));
        assertEquals(Perk.WATER_MASTERY, Perk.byStatAndTier(StatType.WATER_AFFINITY, PerkTier.MASTERY));
        assertEquals(Perk.AIR_TRANSCENDENCE, Perk.byStatAndTier(StatType.AIR_AFFINITY, PerkTier.TRANSCENDENCE));
        assertEquals(Perk.ERUDITION_ACTIVE, Perk.byStatAndTier(StatType.ERUDITION, PerkTier.ACTIVE));
    }
}
