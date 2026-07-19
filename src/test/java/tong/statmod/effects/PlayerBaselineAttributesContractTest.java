package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PlayerBaselineAttributesContractTest {
    @Test
    void appliesStableTransientAdditionsWithoutRefillingResources() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/effects/PlayerAttributeEffects.java"));

        assertTrue(source.contains("Attributes.MAX_HEALTH"));
        assertTrue(source.contains("Attributes.ATTACK_DAMAGE"));
        assertTrue(source.contains("PlayerBaseBalanceRules.BASE_MAX_HEALTH - 20.0D"));
        assertTrue(source.contains("PlayerBaseBalanceRules.BASE_ATTACK_DAMAGE - 1.0D"));
        assertTrue(source.contains("PlayerBaseBalanceRules.manaRegenPerSecond"));
        assertTrue(source.contains("ResourceLocation.fromNamespaceAndPath"));
        assertTrue(source.contains("\"irons_spellbooks\", \"mana_regen\""));
        assertTrue(source.contains("AttributeModifier.Operation.ADDITION"));
        assertFalse(source.contains("setBaseValue"));
        assertTrue(source.contains("double previousMaxHealth = player.getMaxHealth()"));
        assertTrue(source.contains("float previousHealth = player.getHealth()"));
        assertTrue(source.contains("previousHealth / previousMaxHealth"));
        assertTrue(source.contains("player.setHealth"));
        assertFalse(source.contains("setMana"));
    }
}
