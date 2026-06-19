package tong.statmod.integration.mahou;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MahouMappingTest {
    @Test
    void mapsPathFragmentsToElements() {
        assertEquals("earth", MahouElementMapper.elementForPath("mahoutsukai:scroll_rho_aias"));
        assertEquals("fire", MahouElementMapper.elementForPath("mahoutsukai:scroll_fallen_down"));
        assertEquals("air", MahouElementMapper.elementForPath("mahoutsukai:scroll_mental_displacement"));
        assertEquals("earth", MahouElementMapper.elementForPath("mahoutsukai:scroll_boundary_gravity"));
        assertEquals("fire", MahouElementMapper.elementForPath("mahoutsukai:scroll_black_flame"));
        assertEquals("fire", MahouElementMapper.elementForPath("fire_scroll"));
        assertEquals("water", MahouElementMapper.elementForPath("ice_tome"));
        assertEquals("arcane", MahouElementMapper.elementForPath("mystery_scroll"));
    }

    @Test
    void mapsElementsToStats() {
        assertArrayEquals(new StatType[]{StatType.FIRE_AFFINITY, StatType.ARCANE_POWER},
                MahouPerkMap.statsForElement("fire"));
        assertArrayEquals(new StatType[]{StatType.ARCANE_POWER, StatType.ERUDITION},
                MahouPerkMap.statsForElement("arcane"));
        assertArrayEquals(new StatType[]{StatType.EARTH_AFFINITY, StatType.MAGIC_RESISTANCE},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_rho_aias"));
        assertArrayEquals(new StatType[]{StatType.ERUDITION, StatType.MANA_POOL},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_mystic_staff"));
        assertArrayEquals(new StatType[]{StatType.ARCANE_POWER, StatType.MANA_POOL},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_boundary_drain_life"));
        assertArrayEquals(new StatType[]{StatType.CASTING_SPEED, StatType.AIR_AFFINITY},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_mental_displacement"));
        assertArrayEquals(new StatType[]{StatType.EARTH_AFFINITY, StatType.MAGIC_RESISTANCE},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_boundary_gravity"));
        assertArrayEquals(new StatType[]{StatType.ERUDITION, StatType.WILLPOWER},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_prediction"));
        assertArrayEquals(new StatType[]{StatType.FIRE_AFFINITY, StatType.ARCANE_POWER},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_black_flame"));
        assertArrayEquals(new StatType[]{StatType.ERUDITION, StatType.MANA_POOL},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_treasury_projection"));
    }

    @Test
    void scalesTierBonusByScrollName() {
        assertEquals(1, MahouSpellTier.xpMultiplierForPath("basic_scroll"));
        assertEquals(2, MahouSpellTier.xpMultiplierForPath("advanced_scroll"));
        assertEquals(3, MahouSpellTier.xpMultiplierForPath("ultimate_scroll"));
    }

    @Test
    void gatesSpellTiersByArcanePower() {
        assertEquals(0, MahouSpellTier.requiredArcanePowerForPath("basic_scroll"));
        assertEquals(20, MahouSpellTier.requiredArcanePowerForPath("advanced_scroll"));
        assertEquals(40, MahouSpellTier.requiredArcanePowerForPath("ultimate_scroll"));
        assertEquals(40, MahouSpellTier.requiredArcanePowerForPath("master_grimoire"));

        assertEquals(false, MahouSpellTier.canCast(19, "advanced_scroll"));
        assertEquals(true, MahouSpellTier.canCast(20, "advanced_scroll"));
        assertEquals(false, MahouSpellTier.canCast(39, "ultimate_scroll"));
        assertEquals(true, MahouSpellTier.canCast(40, "ultimate_scroll"));
    }

    @Test
    void gatesCuratedProfilesByTheirPrimaryMagicStat() {
        assertEquals(10, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_gandr"));
        assertEquals(15, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_rho_aias"));
        assertEquals(30, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_fallen_down"));
        assertEquals(20, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_mystic_staff"));
        assertEquals(20, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(10, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_mental_displacement"));
        assertEquals(15, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_boundary_gravity"));
        assertEquals(15, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_prediction"));
        assertEquals(20, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_black_flame"));
        assertEquals(20, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_treasury_projection"));

        assertEquals(10, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_gandr"));
        assertEquals(20, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_rho_aias"));
        assertEquals(25, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_mystic_staff"));
        assertEquals(25, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(40, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_fallen_down"));
        assertEquals(15, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_mental_displacement"));
        assertEquals(20, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_boundary_gravity"));
        assertEquals(20, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_prediction"));
        assertEquals(25, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_black_flame"));
        assertEquals(25, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_treasury_projection"));

        assertEquals(false, MahouSpellTier.canCast(39, 30, "mahoutsukai:scroll_fallen_down"));
        assertEquals(false, MahouSpellTier.canCast(40, 29, "mahoutsukai:scroll_fallen_down"));
        assertEquals(true, MahouSpellTier.canCast(40, 30, "mahoutsukai:scroll_fallen_down"));
        assertEquals(false, MahouSpellTier.canCast(24, 20, "mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(true, MahouSpellTier.canCast(25, 20, "mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(false, MahouSpellTier.canCast(14, 10, "mahoutsukai:scroll_mental_displacement"));
        assertEquals(true, MahouSpellTier.canCast(15, 10, "mahoutsukai:scroll_mental_displacement"));
        assertEquals(false, MahouSpellTier.canCast(19, 15, "mahoutsukai:scroll_prediction"));
        assertEquals(true, MahouSpellTier.canCast(20, 15, "mahoutsukai:scroll_prediction"));
        assertEquals(false, MahouSpellTier.canCast(24, 20, "mahoutsukai:scroll_black_flame"));
        assertEquals(true, MahouSpellTier.canCast(25, 20, "mahoutsukai:scroll_black_flame"));
        assertEquals(false, MahouSpellTier.canCast(40, 14, "mahoutsukai:scroll_rho_aias"));
        assertEquals(true, MahouSpellTier.canCast(40, 15, "mahoutsukai:scroll_rho_aias"));
    }

    @Test
    void computesCombatBonusesFromRecentSpellAndStats() {
        assertEquals(1.2f, MahouCompat.earthDamageMultiplier(20, "earth", 200, 210), 0.0001f);
        assertEquals(1.0f, MahouCompat.earthDamageMultiplier(20, "fire", 200, 210), 0.0001f);
        assertEquals(0.3f, MahouCompat.magicReflectionChance(60, 60), 0.0001f);
    }
}
