package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronScrollDescriptorContractTest {
    @Test
    void usesIronPublicApisAndNoAddonAllowlist() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronScrollDescriptor.java"));

        assertTrue(source.contains("instanceof IScroll"));
        assertTrue(source.contains("ISpellContainer.get(stack)"));
        assertTrue(source.contains("SpellRegistry.REGISTRY.get().getKey(spell)"));
        assertTrue(source.contains("spell.getMinLevel()"));
        assertTrue(source.contains("spell.getMaxLevel()"));
        assertFalse(source.contains("wind_spellbooks:"));
        assertFalse(source.contains("gametechbcs_spellbooks:"));
    }
}
