package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tong.statmod.integration.overgeared.OvergearedRecipeGate.MaterialGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // ─── Mission M5 Phase α — Material gates (heated_*) ─────────────────────────────

    @Test
    void materialGate_unknownIngot_returnsNull() {
        assertNull(OvergearedRecipeGate.gateFor(ResourceLocation.parse("minecraft:copper_ingot")));
        assertNull(OvergearedRecipeGate.gateFor(ResourceLocation.parse("foo:bar")));
    }

    @Test
    void materialGate_hihiirokane_isTopTier() {
        MaterialGate gate = OvergearedRecipeGate.gateFor(ResourceLocation.parse("tensura:hihiirokane_ingot"));
        assertNotNull(gate);
        assertEquals(70, gate.forging());
        assertEquals(30, gate.erudition());
        assertEquals(22, gate.arcanePower());
    }

    @Test
    void materialGate_adamantite_belowHihiirokane() {
        MaterialGate gate = OvergearedRecipeGate.gateFor(ResourceLocation.parse("tensura:adamantite_ingot"));
        assertNotNull(gate);
        assertEquals(60, gate.forging());
    }

    @Test
    void materialGate_arcaneRequiresErudition() {
        MaterialGate gate = OvergearedRecipeGate.gateFor(ResourceLocation.parse("irons_spellbooks:arcane_ingot"));
        assertNotNull(gate);
        assertEquals(15, gate.forging());
        assertEquals(5, gate.erudition());
    }

    @Test
    void materialGate_tinIsLowTier() {
        MaterialGate gate = OvergearedRecipeGate.gateFor(ResourceLocation.parse("magistuarmory:tin_ingot"));
        assertNotNull(gate);
        assertEquals(3, gate.forging());
        assertEquals(0, gate.erudition());
        assertEquals(0, gate.arcanePower());
    }

    @Test
    void materialGate_allHeatedMaterials_haveGateEntry() {
        // Source ingots qui doivent avoir une gate (un par heated_<material>_ingot
        // déclaré dans tong.statmod.item.ForgingMaterials).
        String[] sources = {
                "minecraft:gold_ingot",
                "minecraft:diamond",
                "magistuarmory:tin_ingot",
                "magistuarmory:bronze_ingot",
                "irons_spellbooks:pyrium_ingot",
                "irons_spellbooks:arcane_ingot",
                "irons_spellbooks:mithril_ingot",
                "tensura:mithril_ingot",
                "tensura:low_magisteel_ingot",
                "tensura:magisteel_ingot",
                "tensura:pure_magisteel_ingot",
                "tensura:high_magisteel_ingot",
                "tensura:orichalcum_ingot",
                "tensura:adamantite_ingot",
                "tensura:hihiirokane_ingot",
        };
        for (String src : sources) {
            assertNotNull(OvergearedRecipeGate.gateFor(ResourceLocation.parse(src)),
                    "missing gate entry for " + src);
        }
    }

    @Test
    void materialGate_tierProgression_isMonotonicOnForging() {
        // Ordre croissant attendu sur la stat FORGING — chaque tier doit demander
        // ≥ au précédent. Si un tier régresse, la table est mal calibrée.
        String[] ordered = {
                "magistuarmory:tin_ingot",         // 3
                "magistuarmory:bronze_ingot",      // 5
                "minecraft:gold_ingot",            // 8
                "tensura:orichalcum_ingot",        // 12
                "irons_spellbooks:pyrium_ingot",   // 12
                "irons_spellbooks:arcane_ingot",   // 15
                "minecraft:diamond",               // 20
                "irons_spellbooks:mithril_ingot",  // 20
                "tensura:low_magisteel_ingot",     // 25
                "tensura:magisteel_ingot",         // 30
                "minecraft:netherite_ingot",       // 35
                "tensura:pure_magisteel_ingot",    // 40
                "tensura:high_magisteel_ingot",    // 50
                "tensura:adamantite_ingot",        // 60
                "tensura:hihiirokane_ingot",       // 70
        };
        int previous = 0;
        for (String id : ordered) {
            MaterialGate g = OvergearedRecipeGate.gateFor(ResourceLocation.parse(id));
            assertNotNull(g, id);
            assertTrue(g.forging() >= previous,
                    "tier regression at " + id + ": " + g.forging() + " < " + previous);
            previous = g.forging();
        }
    }

    @Test
    void canHeat_unknownIngot_alwaysAllowed() {
        // copper/iron/steel/silver/netherite_alloy gérés par Overgeared natif → on
        // n'en gate aucun côté STAT MOD.
        assertTrue(OvergearedRecipeGate.canHeat(ResourceLocation.parse("minecraft:iron_ingot"), null));
        assertTrue(OvergearedRecipeGate.canHeat(ResourceLocation.parse("minecraft:copper_ingot"), null));
    }
}
