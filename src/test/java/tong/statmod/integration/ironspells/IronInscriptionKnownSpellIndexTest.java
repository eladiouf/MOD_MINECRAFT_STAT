package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronInscriptionKnownSpellIndexTest {
    @Test
    void filtersToKnownIronsSpellsOnly() {
        List<String> ids = IronInscriptionKnownSpellIndex.learnedIronSpellIds(List.of(
                "tensura:fire_bolt",
                "irons_spellbooks:fireball",
                "irons_spellbooks:firebolt",
                "irons_spellbooks:fireball",
                "minecraft:stone"
        ));

        assertEquals(List.of("irons_spellbooks:fireball", "irons_spellbooks:firebolt"), ids);
    }

    @Test
    void buttonIdsRoundTripToOptionIndexes() {
        assertEquals(1000, IronInscriptionKnownSpellIndex.buttonIdForOption(0));
        assertEquals(4, IronInscriptionKnownSpellIndex.optionIndexFromButtonId(1004));
        assertEquals(-1, IronInscriptionKnownSpellIndex.optionIndexFromButtonId(999));
    }

    @Test
    void pagesKnownSpellsInFixedSlices() {
        List<String> page0 = IronInscriptionKnownSpellIndex.page(List.of(
                "a", "b", "c", "d", "e", "f", "g"
        ), 0);
        List<String> page1 = IronInscriptionKnownSpellIndex.page(List.of(
                "a", "b", "c", "d", "e", "f", "g"
        ), 1);

        assertEquals(List.of("a", "b", "c", "d", "e"), page0);
        assertEquals(List.of("f", "g"), page1);
        assertEquals(1, IronInscriptionKnownSpellIndex.maxPage(List.of("a", "b", "c", "d", "e", "f")));
    }
}
