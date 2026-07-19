package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PlayerAttributeEffectsContractTest {
    @Test
    void resolvesOptionalAttributesAndReplacesTransientModifiers() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/effects/PlayerAttributeEffects.java"));

        assertTrue(source.contains("ForgeRegistries.ATTRIBUTES.getValue"));
        assertTrue(source.contains("StaminaAttributeTarget.values()"));
        assertTrue(source.contains("MobilityAttributeTarget.values()"));
        assertTrue(source.contains("for (MagicAttributeTarget target : MagicAttributeTarget.values())"));
        assertTrue(source.contains("AutomaticPerkBonuses bonuses = AutomaticPerkBonuses.from(stats)"));
        assertTrue(source.contains("target.amount(stats, bonuses)"));
        assertTrue(source.contains("target.amount(level, bonuses)"));
        assertTrue(source.contains("replaceModifier("));
        assertTrue(source.contains("removeModifier(modifierId)"));
        assertTrue(source.contains("addTransientModifier"));
        assertFalse(source.contains("addPermanentModifier"));
        assertFalse(source.contains("PERK_MODIFIER"));
        assertTrue(source.contains("AttributeModifier.Operation.MULTIPLY_BASE"));
        assertTrue(source.contains("StatType.PHYSICAL_ENDURANCE"));
        assertFalse(source.contains("yesman.epicfight"));
        assertFalse(source.contains("com.alrex.parcool"));
        assertFalse(source.contains("net.puffish.attributesmod"));
        assertFalse(source.contains("io.redspace.ironsspellbooks"));
    }
}
