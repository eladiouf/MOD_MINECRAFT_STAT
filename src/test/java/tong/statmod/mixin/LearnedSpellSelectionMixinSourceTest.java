package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnedSpellSelectionMixinSourceTest {
    private static final Path SOURCE = Path.of("src", "main", "java", "tong", "statmod",
            "mixin", "SpellSelectionManagerMixin.java");

    @Test
    void learnedSpellsUseStableVirtualSlotAndReplaceEquipmentDuplicates() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("LearnedSpellCastPolicy.SLOT"));
        assertTrue(source.contains("selectionOptionList.removeIf"));
        assertTrue(source.contains("existing.spellData.getSpell().equals(spell)"));
        assertTrue(source.contains("selectionOptionList.add(option)"));
        assertFalse(source.contains("addOrMergeSelectionOption(option)"));
        assertFalse(source.contains("new SelectionOption(\n                    spellData, \"statmod\""));
    }
}
