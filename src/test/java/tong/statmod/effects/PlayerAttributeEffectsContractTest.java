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
        assertTrue(source.contains("removeModifier(target.modifierId())"));
        assertTrue(source.contains("addTransientModifier"));
        assertTrue(source.contains("AttributeModifier.Operation.MULTIPLY_BASE"));
        assertTrue(source.contains("StatType.PHYSICAL_ENDURANCE"));
        assertFalse(source.contains("yesman.epicfight"));
        assertFalse(source.contains("com.alrex.parcool"));
    }
}
