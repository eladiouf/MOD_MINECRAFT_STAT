package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Confirms the same selection code path used by the real Inscription block menu
 * also serves the virtual (block-less) menu — both consume {@link PlayerStatData}
 * via {@link IronInscriptionSelectionService}.
 */
class IronInscriptionSelectionFilteringTest {
    @Test
    void known_iron_spells_are_exposed_filtered() {
        PlayerStatData data = new PlayerStatData();
        data.learnSpell("irons_spellbooks:firebolt");
        data.learnSpell("tensura:dark_lightning");
        data.learnSpell("irons_spellbooks:fireball");
        data.learnSpell("minecraft:stone");

        List<String> known = IronInscriptionSelectionService.knownIronSpellIds(data);
        assertEquals(List.of("irons_spellbooks:fireball", "irons_spellbooks:firebolt"), known);
    }

    @Test
    void no_learned_spells_yields_empty_list() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(List.of(), IronInscriptionSelectionService.knownIronSpellIds(data));
    }

    @Test
    void null_data_is_safe() {
        assertEquals(List.of(), IronInscriptionSelectionService.knownIronSpellIds(null));
    }

    @Test
    void option_index_resolves_to_stable_id() {
        PlayerStatData data = new PlayerStatData();
        data.learnSpell("irons_spellbooks:firebolt");
        data.learnSpell("irons_spellbooks:fireball");

        assertEquals("irons_spellbooks:fireball", IronInscriptionSelectionService.resolveSelectedSpellId(data, 0));
        assertEquals("irons_spellbooks:firebolt", IronInscriptionSelectionService.resolveSelectedSpellId(data, 1));
        assertNull(IronInscriptionSelectionService.resolveSelectedSpellId(data, 2));
        assertNull(IronInscriptionSelectionService.resolveSelectedSpellId(data, -1));
    }
}
