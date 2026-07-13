package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinOptionalCompatPluginTest {
    private static final Path MIXIN_CONFIG = Path.of("src", "main", "resources", "statmod.mixins.json");
    private static final Path PLUGIN_SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "mixin", "StatModMixinPlugin.java");

    @Test
    void mixinConfigUsesPluginToGateOptionalCompatMixins() throws IOException {
        String config = Files.readString(MIXIN_CONFIG);

        assertTrue(config.contains("\"plugin\": \"tong.statmod.mixin.StatModMixinPlugin\""));
        assertTrue(Files.exists(PLUGIN_SOURCE));
    }

    @Test
    void pluginMapsEveryOptionalMixinToItsOwningMod() throws IOException {
        String source = Files.readString(PLUGIN_SOURCE);

        Map<String, String> optionalMixins = Map.ofEntries(
                Map.entry("OvergearedSmithingScreenMixin", "overgeared"),
                Map.entry("OvergearedSmithingMixin", "overgeared"),
                Map.entry("OvergearedSmithingMenuMixin", "overgeared"),
                Map.entry("OvergearedForgingRecipeMixin", "overgeared"),
                Map.entry("OvergearedAlloySmelterMixin", "overgeared"),
                Map.entry("EpicFightSkillBookScreenMixin", "epicfight"),
                Map.entry("EpicFightPlayerScaleMixin", "epicfight"),
                Map.entry("ItemKeywordReloadListenerMixin", "epicfight"),
                Map.entry("EpicFightSkillBuilderMixin", "epicfight"),
                Map.entry("EpicFightPlayerSkillsMixin", "epicfight"),
                Map.entry("EpicFightNullAnimationMixin", "epicfight"),
                Map.entry("IronInscriptionTableScreenMixin", "irons_spellbooks"),
                Map.entry("IronInscriptionTableMenuMixin", "irons_spellbooks"),
                Map.entry("IronSpellIconResolverMixin", "irons_spellbooks"),
                Map.entry("IronLearnedSpellCastSourceMixin", "irons_spellbooks"),
                Map.entry("PuffishSkillsScreenMixin", "puffish_skills"),
                Map.entry("ReincarnationMenuRaceFilterMixin", "tensura")
        );

        for (Map.Entry<String, String> entry : optionalMixins.entrySet()) {
            assertTrue(source.contains("Map.entry(\"" + entry.getKey() + "\", \"" + entry.getValue() + "\")"),
                    entry.getKey() + " must be gated by " + entry.getValue());
        }

        assertTrue(source.contains("LoadingModList.get()"));
        assertTrue(source.contains("list.getModFileById(modid) != null"));
        assertFalse(source.contains("Map.entry(\"PlayerListMixin\""));
        assertFalse(source.contains("Map.entry(\"ItemStackDurabilityMixin\""));
        assertFalse(source.contains("Map.entry(\"AssemblyCraftingResultMixin\""));
    }
}
