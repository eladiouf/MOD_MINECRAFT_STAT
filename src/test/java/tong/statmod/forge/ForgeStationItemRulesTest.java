package tong.statmod.forge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationItemRulesTest {

    @Test
    void rules_identifyKnownInfusionFamiliesByItemId() {
        assertTrue(ForgeStationItemRules.isRoughIntermediateId("statmod:rough_blade_arcane"));
        assertTrue(ForgeStationItemRules.isRoughIntermediateId("statmod:rough_dagger_blade_arcane"));
        assertTrue(ForgeStationItemRules.isRoughIntermediateId("overgeared:iron_sword_blade"));
        assertTrue(ForgeStationItemRules.isRoughIntermediateId("overgeared:steel_axe_head"));
        assertTrue(ForgeStationItemRules.isRuneEssenceId("statmod:rune_essence_arcane"));
        assertTrue(ForgeStationItemRules.isGripId("statmod:runic_grip"));
        assertTrue(ForgeStationItemRules.isShardLikeCatalystId("slu:flame_shard"));
        assertTrue(ForgeStationItemRules.isShardLikeCatalystId("simplyswords:runefused_gem"));
        assertTrue(ForgeStationItemRules.isAnvilSupportId("statmod:basic_forge_tongs"));
        assertTrue(ForgeStationItemRules.isAnvilSupportId("statmod:basic_smithing_hammer"));

        assertFalse(ForgeStationItemRules.isRoughIntermediateId("minecraft:diamond_sword"));
        assertFalse(ForgeStationItemRules.isRuneEssenceId("slu:flame_shard"));
        assertFalse(ForgeStationItemRules.isGripId("minecraft:stick"));
        assertFalse(ForgeStationItemRules.isShardLikeCatalystId("minecraft:stick"));
        assertFalse(ForgeStationItemRules.isAnvilSupportId("statmod:runic_grip"));
    }
}
