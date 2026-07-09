package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSpellAdvancedPerkSourceTest {
    @Test
    void eventBridgeReadsAdvancedDamageAndElementalPerks() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java"));

        assertTrue(source.contains("IronSpellAdvancedPerkScaling.arcaneDamageMultiplier"));
        assertTrue(source.contains("IronSpellAdvancedPerkScaling.elementalDamageMultiplier"));
        assertTrue(source.contains("Perk.ARCANE_ACTIVE.id"));
        assertTrue(source.contains("Perk.ARCANE_TRANSCENDENCE.id"));
        assertTrue(source.contains("Perk.FIRE_ACTIVE.id"));
        assertTrue(source.contains("Perk.WATER_ACTIVE.id"));
        assertTrue(source.contains("Perk.EARTH_ACTIVE.id"));
        assertTrue(source.contains("Perk.AIR_ACTIVE.id"));
    }

    @Test
    void attributeBridgeReadsAdvancedMagicCorePerks() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellAttributeBridge.java"));

        assertTrue(source.contains("IronSpellAdvancedPerkScaling.advancedPercentAttributeBonus"));
        assertTrue(source.contains("IronSpellAdvancedPerkScaling.advancedManaBonus"));
        assertTrue(source.contains("Perk.MAGIC_RESIST_TRANSCENDENCE.id"));
        assertTrue(source.contains("Perk.CASTING_SPEED_TRANSCENDENCE.id"));
        assertTrue(source.contains("Perk.MANA_POOL_TRANSCENDENCE.id"));
        assertTrue(source.contains("Perk.ERUDITION_TRANSCENDENCE.id"));
    }

    @Test
    void ironManaIsRestoredOnWakeWithoutRemovingCastRefunds() throws IOException {
        String compat = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellsCompat.java"));
        String recovery = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellRestRecoveryHandler.java"));
        String eventBridge = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java"));

        assertTrue(compat.contains("NeoForge.EVENT_BUS.register(IronSpellRestRecoveryHandler.class)"));
        assertTrue(recovery.contains("PlayerWakeUpEvent"));
        assertTrue(recovery.contains("IronSpellManaSyncBridge.restoreToMax(player)"));
        assertTrue(eventBridge.contains("IronSpellAdvancedPerkScaling.manaRefund"));
        assertTrue(eventBridge.contains("IronSpellManaSyncBridge.addMana(player"));
    }

    @Test
    void attributeBridgeKeepsNativePassiveManaRegenActiveWithoutRemovingCastRefunds() throws IOException {
        String attributeBridge = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellAttributeBridge.java"));
        String eventBridge = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java"));

        assertTrue(attributeBridge.contains("MANA_REGEN"));
        assertTrue(attributeBridge.contains("manaRegenBonus"),
                "STAT scaling should still contribute a mana regen bonus on top of Iron's native regen");
        assertFalse(attributeBridge.contains("neutralizeBaseManaRegen("),
                "Iron's native passive mana regen should remain active.");
        assertFalse(attributeBridge.contains("regenMana(serverPlayer);"),
                "STAT Mod should not double-regenerate mana once Iron's native regen is reactivated.");
        assertTrue(eventBridge.contains("IronSpellManaSyncBridge.addMana(player"));
    }
}
