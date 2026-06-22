package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
