package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IronLearnedSpellCastSourceMixinTest {
    @Test
    void virtualLearnedSlotReturnsSpellbookCastSource() throws Exception {
        Path source = Path.of("src", "main", "java", "tong", "statmod", "mixin",
                "IronLearnedSpellCastSourceMixin.java");
        String text = Files.readString(source);

        assertTrue(text.contains("LearnedSpellCastPolicy.isVirtualSlot(this.slot)"));
        assertTrue(text.contains("cir.setReturnValue(CastSource.SPELLBOOK)"));
    }

    @Test
    void castSourceMixinIsRegistered() throws Exception {
        String config = Files.readString(Path.of("src", "main", "resources", "statmod.mixins.json"));
        String plugin = Files.readString(Path.of("src", "main", "java", "tong", "statmod",
                "mixin", "StatModMixinPlugin.java"));

        assertTrue(config.contains("\"IronLearnedSpellCastSourceMixin\""));
        assertTrue(plugin.contains("Map.entry(\"IronLearnedSpellCastSourceMixin\", \"irons_spellbooks\")"));
    }
}
