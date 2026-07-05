package tong.statmod.stats;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingSupportEffectHandlerTest {
    @Test
    void forgingCookingAndAlchemyScalingAreStandaloneStatEffects() {
        assertEquals(10, CraftingSupportEffectHandler.reducedDurabilityLoss(10, 0));
        assertEquals(5, CraftingSupportEffectHandler.reducedDurabilityLoss(10, 50));
        assertEquals(0, CraftingSupportEffectHandler.reducedDurabilityLoss(10, 500));
        assertEquals(1.40f, CraftingSupportEffectHandler.potionDurationMultiplier(40), 1.0e-6f);
        assertEquals(1.50f, CraftingSupportEffectHandler.foodSaturationBonus(30), 1.0e-6f);
    }

    @Test
    void cookingAndAlchemyCorePerksApplyTheirAdvertisedBonuses() {
        assertEquals(2.50f, CraftingSupportEffectHandler.foodSaturationBonus(30, true), 1.0e-6f);
        assertEquals(1.50f, CraftingSupportEffectHandler.potionDurationMultiplier(40, true), 1.0e-6f);
    }

    @Test
    void alchemyMasteryStrengthensPotionAmplifiersByHalfAVisibleLevelCurve() {
        assertEquals(0, CraftingSupportEffectHandler.potionAmplifier(0, false));
        assertEquals(1, CraftingSupportEffectHandler.potionAmplifier(0, true));
        assertEquals(2, CraftingSupportEffectHandler.potionAmplifier(1, true));
        assertEquals(4, CraftingSupportEffectHandler.potionAmplifier(2, true));
        assertEquals(255, CraftingSupportEffectHandler.potionAmplifier(255, true));
    }

    @Test
    void cookingActiveAndSituationalPerksAdjustFoodSideEffects() {
        assertEquals(32, CraftingSupportEffectHandler.foodUseDurationTicks(32, false));
        assertEquals(16, CraftingSupportEffectHandler.foodUseDurationTicks(32, true));
        assertEquals(1, CraftingSupportEffectHandler.foodUseDurationTicks(1, true));

        assertEquals(200, CraftingSupportEffectHandler.badFoodEffectDurationTicks(200, false));
        assertEquals(100, CraftingSupportEffectHandler.badFoodEffectDurationTicks(200, true));
        assertEquals(1, CraftingSupportEffectHandler.badFoodEffectDurationTicks(1, true));
    }

    @Test
    void alchemySituationalAndTranscendencePerksAdjustPotionDurations() {
        assertEquals(140, CraftingSupportEffectHandler.potionEffectDurationTicks(
                100, 40, false, false, false, false));
        assertEquals(210, CraftingSupportEffectHandler.potionEffectDurationTicks(
                100, 40, false, true, false, true));
        assertEquals(140, CraftingSupportEffectHandler.potionEffectDurationTicks(
                100, 40, false, true, false, false));
        assertEquals(24_000, CraftingSupportEffectHandler.potionEffectDurationTicks(
                100, 0, false, false, true, false));
    }

    @Test
    void alchemyActiveCanDuplicateBrewedPotionOutputs() {
        assertEquals(1, CraftingSupportEffectHandler.brewedPotionOutputCount(false, 0.0f));
        assertEquals(2, CraftingSupportEffectHandler.brewedPotionOutputCount(true, 0.0f));
        assertEquals(2, CraftingSupportEffectHandler.brewedPotionOutputCount(true, 0.149f));
        assertEquals(1, CraftingSupportEffectHandler.brewedPotionOutputCount(true, 0.15f));
    }

    @Test
    void forgingPerksAdjustAnvilCostsRepairOutputAndSharpeningWindows() {
        assertEquals(10, CraftingSupportEffectHandler.anvilLevelCost(10, false, false));
        assertEquals(9, CraftingSupportEffectHandler.anvilLevelCost(10, true, false));
        assertEquals(8, CraftingSupportEffectHandler.anvilLevelCost(10, false, true));
        assertEquals(7, CraftingSupportEffectHandler.anvilLevelCost(10, true, true));
        assertEquals(1, CraftingSupportEffectHandler.anvilLevelCost(1, true, true));

        assertEquals(1, CraftingSupportEffectHandler.forgeRepairOutputCount(false, 0.0f));
        assertEquals(2, CraftingSupportEffectHandler.forgeRepairOutputCount(true, 0.0f));
        assertEquals(1, CraftingSupportEffectHandler.forgeRepairOutputCount(true, 0.15f));

        assertEquals(12, CraftingSupportEffectHandler.perfectRepairDamageValue(12, false));
        assertEquals(0, CraftingSupportEffectHandler.perfectRepairDamageValue(12, true));
        assertEquals(1.10f, CraftingSupportEffectHandler.sharpeningDamageMultiplier(true, true), 1.0e-6f);
        assertEquals(1.0f, CraftingSupportEffectHandler.sharpeningDamageMultiplier(true, false), 1.0e-6f);
    }

    @Test
    void alchemySynergyCanCreateTransmutationBonusOnCraft() {
        assertEquals(0, CraftingSupportEffectHandler.transmutationBonusCount(false, 0.0f));
        assertEquals(1, CraftingSupportEffectHandler.transmutationBonusCount(true, 0.0f));
        assertEquals(1, CraftingSupportEffectHandler.transmutationBonusCount(true, 0.099f));
        assertEquals(0, CraftingSupportEffectHandler.transmutationBonusCount(true, 0.10f));
    }

    @Test
    void craftingSupportEffectsAreRegisteredWithoutTensura() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/STATMod.java"));
        int registerIndex = source.indexOf("NeoForge.EVENT_BUS.register(CraftingSupportEffectHandler.class);");
        int tensuraGateIndex = source.indexOf("if (tensuraLoaded)");

        assertTrue(registerIndex >= 0, "Crafting support effects must be registered on the main event bus.");
        assertTrue(tensuraGateIndex >= 0, "The Tensura gate should still exist for Tensura-only handlers.");
        assertTrue(registerIndex < tensuraGateIndex,
                "COOKING/ALCHEMY/FORGING effects are core STAT effects and must not depend on Tensura being loaded.");
    }

    @Test
    void handlerReadsCraftingCorePerksFromPlayerData() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/stats/CraftingSupportEffectHandler.java"));

        assertTrue(source.contains("Perk.COOK_CORE.id"),
                "COOK_CORE must affect the food bonus promised by the perk description.");
        assertTrue(source.contains("Perk.COOK_ACTIVE.id"),
                "COOK_ACTIVE must halve bad food style negative effects.");
        assertTrue(source.contains("Perk.COOK_SITUATIONAL.id"),
                "COOK_SITUATIONAL must speed up eating.");
        assertTrue(source.contains("Perk.COOK_MASTERY.id"),
                "COOK_MASTERY must share food effects when food is eaten.");
        assertTrue(source.contains("Perk.COOK_TRANSCENDENCE.id"),
                "COOK_TRANSCENDENCE must give regeneration from food.");
        assertTrue(source.contains("Perk.ALCHEM_ACTIVE.id"),
                "ALCHEM_ACTIVE must affect brewed potion output.");
        assertTrue(source.contains("Perk.ALCHEM_CORE.id"),
                "ALCHEM_CORE must affect the potion duration promised by the perk description.");
        assertTrue(source.contains("Perk.ALCHEM_SITUATIONAL.id"),
                "ALCHEM_SITUATIONAL must affect poison potion durations.");
        assertTrue(source.contains("Perk.ALCHEM_MASTERY.id"),
                "ALCHEM_MASTERY must affect potion strength promised by the perk description.");
        assertTrue(source.contains("Perk.ALCHEM_TRANSCENDENCE.id"),
                "ALCHEM_TRANSCENDENCE must make potion effects meaningfully long-lived.");
        assertTrue(source.contains("Perk.ALCHEM_SYNERGY.id"),
                "ALCHEM_SYNERGY must convert materials on craft.");
        assertTrue(source.contains("Perk.FORGE_ACTIVE.id"),
                "FORGE_ACTIVE must affect repair output.");
        assertTrue(source.contains("Perk.FORGE_SYNERGY.id"),
                "FORGE_SYNERGY must reduce anvil costs.");
        assertTrue(source.contains("Perk.FORGE_TRANSCENDENCE.id"),
                "FORGE_TRANSCENDENCE must repair items to perfect condition.");
    }

    @Test
    void handlerUsesEventsThatCanImplementSupportPerks() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/stats/CraftingSupportEffectHandler.java"));

        assertTrue(source.contains("LivingEntityUseItemEvent.Start"),
                "Fast Food needs the start event because Finish is already too late.");
        assertTrue(source.contains("MobEffectEvent.Added"),
                "Iron Stomach needs the added-effect event to reduce negative durations.");
        assertTrue(source.contains("PlayerBrewedPotionEvent"),
                "Brewer's Secret needs the brewing event to duplicate potion output.");
        assertTrue(source.contains("AnvilUpdateEvent"),
                "Forge cost and perfect repair effects need the anvil update event.");
        assertTrue(source.contains("AnvilRepairEvent"),
                "Forge repair output and sharpening windows need the anvil repair event.");
        assertTrue(source.contains("PlayerEvent.ItemCraftedEvent"),
                "Alchemy transmutation needs the crafted-item event.");
    }

    @Test
    void cookingFoodEffectsAreNotTriggeredByCombatKills() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/perks/PerkEffectHandler.java"));
        int deathIndex = source.indexOf("public static void onLivingDeath");
        int xpIndex = source.indexOf("public static void onLivingExperienceDrop");
        String deathBody = source.substring(deathIndex, xpIndex);

        assertFalse(deathBody.contains("Perk.COOK_MASTERY"),
                "COOK_MASTERY is a food perk and should not trigger from combat kills.");
    }
}
