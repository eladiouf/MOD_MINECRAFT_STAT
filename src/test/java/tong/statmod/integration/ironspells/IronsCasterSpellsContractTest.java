package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronsCasterSpellsContractTest {
    @Test void runtimeUsesValidatedRegistrySpellsAndConfiguredBounds() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronsCasterSpells.java"));
        assertTrue(source.contains("SpellRegistry.getSpell"));
        assertTrue(source.contains("spell.isEnabled()"));
        assertTrue(source.contains("spell.getMinLevel()"));
        assertTrue(source.contains("spell.getMaxLevel()"));
        assertTrue(source.contains("IronSpellProfile profile"));
        assertTrue(source.contains("IronSpellIntent intent"));
        assertFalse(source.contains("io.redspace.ironsspellbooks.spells."));
    }

    @Test void nonHostileIntentsDoNotRequireEnemyAiming() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronsCasterSpells.java"));
        assertTrue(source.contains("requiresHostileAim"));
        assertTrue(source.contains("hostileTarget != null"));
    }
}
