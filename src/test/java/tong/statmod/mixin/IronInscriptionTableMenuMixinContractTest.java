package tong.statmod.mixin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronInscriptionTableMenuMixinContractTest {
    @Test
    void menuSelectionAndBindingAreRevalidatedServerSide() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/mixin/IronInscriptionTableMenuMixin.java"));

        assertTrue(source.contains(
                "method = {\"clickMenuButton\", \"m_6366_\"}"));
        assertTrue(source.contains("LearnedSpellBindingPolicy.optionFromButton(buttonId)"));
        assertTrue(source.contains("StatCapabilities.get(player)"));
        assertTrue(source.contains("IronLearnedSpellBindingService.bind"));
        assertTrue(source.contains("this.selectedSpellIndex"));
        assertFalse(source.contains("clientLevel"));
    }
}
