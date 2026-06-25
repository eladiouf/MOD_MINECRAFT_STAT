package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronInscriptionKnownSpellIndexTest {
    @Test
    void filtersToCastableSpellsIronsAndTensuraWrappers() {
        List<String> ids = IronInscriptionKnownSpellIndex.learnedCastableSpellIds(List.of(
                "tensura:fire_bolt",                  // raw tensura: filtered out
                "irons_spellbooks:fireball",
                "irons_spellbooks:firebolt",
                "irons_spellbooks:fireball",          // duplicate dropped
                "statmod:tensura_fire_bolt",          // wrapper kept
                "statmod:tensura_hellfire",
                "minecraft:stone"
        ));

        assertEquals(List.of(
                "irons_spellbooks:fireball",
                "irons_spellbooks:firebolt",
                "statmod:tensura_fire_bolt",
                "statmod:tensura_hellfire"
        ), ids);
    }

    @Test
    void deprecatedAliasReturnsTheSameResult() {
        List<String> via = IronInscriptionKnownSpellIndex.learnedIronSpellIds(List.of(
                "irons_spellbooks:firebolt",
                "statmod:tensura_fire_bolt"
        ));
        assertEquals(List.of("irons_spellbooks:firebolt", "statmod:tensura_fire_bolt"), via);
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

    @Test
    void rawTensuraIdsAreRejectedSinceTheyAreNotInIronsRegistry() {
        List<String> ids = IronInscriptionKnownSpellIndex.learnedCastableSpellIds(List.of(
                "tensura:hellfire",
                "tensura:water_jail"
        ));
        assertTrue(ids.isEmpty());
    }

    @Test
    void filteredPagingAppliesPredicateBeforePaginating() {
        List<String> spells = List.of(
                "fireball",
                "frostbolt",
                "firebolt",
                "heal",
                "firestorm",
                "blink",
                "fire_wave"
        );
        Predicate<String> fireOnly = id -> id.contains("fire");

        List<String> page0 = IronInscriptionKnownSpellIndex.page(spells, 0, fireOnly);
        List<String> page1 = IronInscriptionKnownSpellIndex.page(spells, 1, fireOnly);

        assertEquals(List.of("fireball", "firebolt", "firestorm", "fire_wave"), page0);
        assertEquals(List.of(), page1);
        assertEquals(0, IronInscriptionKnownSpellIndex.maxPage(spells, fireOnly));
    }

    @Test
    void optionIndexOfReturnsStableIndexFromUnfilteredList() {
        List<String> known = List.of(
                "irons_spellbooks:chain_lightning",
                "irons_spellbooks:fireball",
                "irons_spellbooks:firebolt",
                "irons_spellbooks:heal"
        );

        assertEquals(2, IronInscriptionKnownSpellIndex.optionIndexOf(known, "irons_spellbooks:firebolt"));
        assertEquals(-1, IronInscriptionKnownSpellIndex.optionIndexOf(known, "irons_spellbooks:blink"));
    }

    @Test
    void normalizeSelectedOptionClearsSelectionWhenHiddenByFilters() {
        List<String> known = List.of(
                "irons_spellbooks:fireball",
                "irons_spellbooks:heal",
                "irons_spellbooks:ice_spike"
        );
        Predicate<String> fireOnly = id -> id.contains("fire");

        assertEquals(0, IronInscriptionKnownSpellIndex.normalizeSelectedOption(known, 0, fireOnly));
        assertEquals(-1, IronInscriptionKnownSpellIndex.normalizeSelectedOption(known, 1, fireOnly));
        assertEquals(-1, IronInscriptionKnownSpellIndex.normalizeSelectedOption(known, 99, fireOnly));
    }
}
