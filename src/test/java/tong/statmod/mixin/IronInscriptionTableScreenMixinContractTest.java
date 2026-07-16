package tong.statmod.mixin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronInscriptionTableScreenMixinContractTest {
    @Test
    void screenUsesLearnedCacheDynamicSchoolsAndRealSearch() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java"));

        assertTrue(source.contains("ClientStatsCache.state().learnedSpells()"));
        assertTrue(source.contains("IronKnownSpellIndex.schools"));
        assertTrue(source.contains("IronKnownSpellIndex.visible"));
        assertTrue(source.contains("EditBox"));
        assertTrue(source.contains("LearnedSpellBindingPolicy.buttonForOption"));
        assertTrue(source.contains("int baseX = Math.max(4, leftPos - 122)"));
        assertTrue(source.contains("method = {\"init\", \"m_7856_\"}"));
        assertTrue(source.contains("method = {\"render\", \"m_88315_\"}"));
        assertTrue(source.contains("method = \"isValidInscription\""));
        assertTrue(source.contains("method = \"onInscription\""));
        assertFalse(source.contains("new SchoolFilterSpec(\"fire\""));
    }
}
