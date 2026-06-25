package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicNodeStatRequirementsTest {

    // ----- D2 universal thresholds -----

    @Test
    void universal_thresholds_T1_signature() {
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T1, SpellRole.ELEMENTAL_DAMAGE_FIRE);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        assertEquals(2, req.arcane().minLevel());
        assertEquals(2, req.erudition().minLevel());
    }

    @Test
    void universal_thresholds_T3_signature() {
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T3, SpellRole.AOE_BLAST);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        assertEquals(6, req.arcane().minLevel());
        assertEquals(5, req.erudition().minLevel());
    }

    @Test
    void universal_thresholds_T1_trunk_foundation() {
        MagicNode node = trunk(MagicTier.T1);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        assertEquals(1, req.arcane().minLevel());
        assertEquals(1, req.erudition().minLevel());
    }

    // ----- D1 spell-role mapping -----

    @Test
    void mobility_spell_tertiary_is_agility() {
        MagicNode burningDash = signature(MagicBranch.FIRE, MagicTier.T2, SpellRole.MOBILITY);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(burningDash);
        assertEquals(StatType.AGILITY, req.tertiary().stat());
    }

    @Test
    void aoe_spell_tertiary_is_intimidation_regardless_of_branch() {
        MagicNode hellfire = signature(MagicBranch.FIRE, MagicTier.T3, SpellRole.AOE_BLAST);
        MagicNode blizzard = signature(MagicBranch.WATER, MagicTier.T3, SpellRole.AOE_BLAST);
        assertEquals(StatType.INTIMIDATION,
                MagicNodeStatRequirements.forNode(hellfire).tertiary().stat());
        assertEquals(StatType.INTIMIDATION,
                MagicNodeStatRequirements.forNode(blizzard).tertiary().stat());
    }

    @Test
    void trunk_foundation_has_no_tertiary() {
        MagicNode node = trunk(MagicTier.T2);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        assertNull(req.tertiary(), "TRUNK_FOUNDATION should have no tertiary gate");
    }

    // ----- D1 structural (opener/tier) -----

    @Test
    void fire_opener_tertiary_is_fire_affinity_2() {
        MagicNode node = opener(MagicBranch.FIRE);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        assertNotNull(req.tertiary());
        assertEquals(StatType.FIRE_AFFINITY, req.tertiary().stat());
        assertEquals(2, req.tertiary().minLevel());
    }

    @Test
    void fire_tier_T3_switches_to_intimidation_archetype() {
        MagicNode node = tier(MagicBranch.FIRE, MagicTier.T3);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        assertNotNull(req.tertiary());
        assertEquals(StatType.INTIMIDATION, req.tertiary().stat(),
                "T3 should pivot from element affinity to archetype identity");
        assertEquals(5, req.tertiary().minLevel());
    }

    @Test
    void air_tier_T3_uses_agility_archetype() {
        MagicNode node = tier(MagicBranch.AIR, MagicTier.T3);
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        assertEquals(StatType.AGILITY, req.tertiary().stat());
    }

    @Test
    void ender_keeps_keen_senses_at_all_tiers() {
        for (MagicTier tier : new MagicTier[]{MagicTier.T1, MagicTier.T2, MagicTier.T3}) {
            MagicNode node = tier(MagicBranch.ENDER, tier);
            MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
            assertEquals(StatType.KEEN_SENSES, req.tertiary().stat(),
                    "Ender tertiary should remain KEEN_SENSES at " + tier);
        }
    }

    // ----- D3 race deltas on universal gates -----

    @Test
    void human_gets_arcane_and_erudition_discount() {
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T1, SpellRole.ELEMENTAL_DAMAGE_FIRE);
        PlayerStatData data = playerData(MagicRace.HUMAN, MagicBranch.FIRE);

        int effectiveArcane = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).arcane(), data, node);
        int effectiveErudition = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).erudition(), data, node);
        assertEquals(1, effectiveArcane, "HUMAN -1 on ARCANE_POWER (base 2)");
        assertEquals(1, effectiveErudition, "HUMAN -1 on ERUDITION (base 2)");
    }

    @Test
    void elf_gets_erudition_discount_only() {
        MagicNode node = signature(MagicBranch.AIR, MagicTier.T2, SpellRole.ELEMENTAL_DAMAGE_AIR);
        PlayerStatData data = playerData(MagicRace.ELF, MagicBranch.AIR);

        int effectiveArcane = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).arcane(), data, node);
        int effectiveErudition = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).erudition(), data, node);
        assertEquals(4, effectiveArcane, "ELF no arcane discount");
        assertEquals(1, effectiveErudition, "ELF -2 on ERUDITION (base 3)");
    }

    @Test
    void dwarf_gets_arcane_discount_only() {
        MagicNode node = signature(MagicBranch.EARTH, MagicTier.T1, SpellRole.ELEMENTAL_DAMAGE_EARTH);
        PlayerStatData data = playerData(MagicRace.DWARF, MagicBranch.EARTH);

        int effectiveArcane = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).arcane(), data, node);
        int effectiveErudition = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).erudition(), data, node);
        assertEquals(1, effectiveArcane, "DWARF -1 on ARCANE_POWER (base 2)");
        assertEquals(2, effectiveErudition, "DWARF no erudition discount");
    }

    @Test
    void beast_pays_erudition_penalty() {
        MagicNode node = signature(MagicBranch.WATER, MagicTier.T1, SpellRole.ELEMENTAL_DAMAGE_WATER);
        PlayerStatData data = playerData(MagicRace.BEAST, MagicBranch.WATER);

        int effectiveErudition = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).erudition(), data, node);
        assertEquals(3, effectiveErudition, "BEAST +1 on ERUDITION (base 2)");
    }

    // ----- D3 race deltas on tertiary -----

    @Test
    void dwarf_fire_affinity_tertiary_gets_discount() {
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T1, SpellRole.ELEMENTAL_DAMAGE_FIRE);
        PlayerStatData data = playerData(MagicRace.DWARF, null);

        int effective = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).tertiary(), data, node);
        assertEquals(0, effective,
                "DWARF affinité Fire → -1 on FIRE_AFFINITY threshold (1-1=0)");
    }

    @Test
    void start_branch_discount_on_tertiary() {
        MagicNode node = signature(MagicBranch.AIR, MagicTier.T2, SpellRole.MOBILITY);
        PlayerStatData data = playerData(MagicRace.HUMAN, MagicBranch.AIR);

        int effective = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).tertiary(), data, node);
        // AGILITY base T2 = 3, start branch Air → -1 → 2
        assertEquals(2, effective, "Start branch should give -1 to tertiary threshold");
    }

    @Test
    void beast_purity_penalty_on_non_affine_branch() {
        // Beast affinities = Water + Air. Fire is non-affine → +1 penalty.
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T1, SpellRole.AOE_BLAST);
        PlayerStatData data = playerData(MagicRace.BEAST, MagicBranch.WATER);

        int effective = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).tertiary(), data, node);
        // INTIMIDATION base T1 = 1, Beast purity penalty Fire = +1 → 2
        assertEquals(2, effective);
    }

    @Test
    void thresholds_clamp_to_zero() {
        // Dwarf + start branch Fire + Fire affinity tertiary → 1 -1 -1 = -1, clamped to 0
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T1, SpellRole.ELEMENTAL_DAMAGE_FIRE);
        PlayerStatData data = playerData(MagicRace.DWARF, MagicBranch.FIRE);

        int effective = MagicNodeStatRequirements.effectiveThreshold(
                MagicNodeStatRequirements.forNode(node).tertiary(), data, node);
        assertEquals(0, effective, "Negative effective threshold must clamp to 0");
    }

    // ----- All-gates check -----

    @Test
    void player_meeting_all_gates_passes() {
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T1, SpellRole.ELEMENTAL_DAMAGE_FIRE);
        PlayerStatData data = playerData(MagicRace.HUMAN, MagicBranch.FIRE);
        data.setLevel(StatType.ARCANE_POWER.index, 5);
        data.setLevel(StatType.ERUDITION.index, 5);
        data.setLevel(StatType.FIRE_AFFINITY.index, 5);
        assertTrue(MagicNodeStatRequirements.playerMeetsAllGates(node, data));
    }

    @Test
    void player_missing_one_gate_fails() {
        MagicNode node = signature(MagicBranch.FIRE, MagicTier.T3, SpellRole.AOE_BLAST);
        PlayerStatData data = playerData(MagicRace.HUMAN, MagicBranch.FIRE);
        data.setLevel(StatType.ARCANE_POWER.index, 5);   // base 6 - 1 (human) = 5 → meets
        data.setLevel(StatType.ERUDITION.index, 4);      // base 5 - 1 (human) = 4 → meets
        data.setLevel(StatType.INTIMIDATION.index, 3);   // base 5 - 1 (start) = 4 → 3 < 4 FAIL
        assertFalse(MagicNodeStatRequirements.playerMeetsAllGates(node, data));
    }

    // ----- Helpers -----

    private static MagicNode signature(MagicBranch branch, MagicTier tier, SpellRole role) {
        return new MagicNode(
                branch.id + "/signature/test_" + role.name().toLowerCase(),
                branch, MagicNodeKind.SIGNATURE_SPELL, tier,
                MagicCurrency.SCHOOL, 1, List.of(), Set.of(),
                role);
    }

    private static MagicNode opener(MagicBranch branch) {
        return new MagicNode(
                branch.id + "/opener/test",
                branch, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of(), Set.of(),
                SpellRole.BRANCH_OPENER);
    }

    private static MagicNode tier(MagicBranch branch, MagicTier tier) {
        return new MagicNode(
                branch.id + "/tier/test_" + tier.name().toLowerCase(),
                branch, MagicNodeKind.BRANCH_TIER, tier,
                MagicCurrency.SCHOOL, 1, List.of(), Set.of(),
                SpellRole.BRANCH_TIER);
    }

    private static MagicNode trunk(MagicTier tier) {
        return new MagicNode(
                "common/foundation/test_" + tier.name().toLowerCase(),
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, tier,
                MagicCurrency.ARCANE, 1, List.of(), Set.of(),
                SpellRole.TRUNK_FOUNDATION);
    }

    private static PlayerStatData playerData(MagicRace race, MagicBranch chosenStart) {
        PlayerStatData data = new PlayerStatData();
        data.setMagicRace(race);
        data.setChosenStartBranch(chosenStart);
        return data;
    }
}
