package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CleanFoundationVerifierContractTest {
    @Test
    void requiresIronMagicIntegrationClassesInBuiltJar() throws Exception {
        String source = Files.readString(Path.of("scripts/verify-clean-foundation.ps1"));

        assertTrue(source.contains("tong/statmod/effects/MagicAttributeTarget.class"));
        assertTrue(source.contains("tong/statmod/config/StatModConfigEvents.class"));
        assertTrue(source.contains(
                "tong/statmod/integration/ironspells/IronSpellXpEvents.class"));
        assertTrue(source.contains("io/redspace/ironsspellbooks/"));
    }
}
