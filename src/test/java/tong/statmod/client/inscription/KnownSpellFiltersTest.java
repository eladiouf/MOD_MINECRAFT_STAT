package tong.statmod.client.inscription;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnownSpellFiltersTest {
    @Test
    void matchesSearchAgainstDisplayNameOrSpellId() {
        KnownSpellFilters.Criteria criteria = new KnownSpellFilters.Criteria("fire", Set.of());

        assertTrue(KnownSpellFilters.matches(
                "irons_spellbooks:fireball",
                criteria,
                id -> "Fireball",
                id -> "fire"));
        assertTrue(KnownSpellFilters.matches(
                "statmod:tensura_fire_bolt",
                criteria,
                id -> "Bolt",
                id -> "fire"));
        assertFalse(KnownSpellFilters.matches(
                "irons_spellbooks:heal",
                criteria,
                id -> "Healing Circle",
                id -> "holy"));
    }

    @Test
    void schoolFilterRejectsSpellsOutsideSelectedSchools() {
        KnownSpellFilters.Criteria criteria = new KnownSpellFilters.Criteria("", Set.of("fire", "holy"));

        assertTrue(KnownSpellFilters.matches(
                "irons_spellbooks:fireball",
                criteria,
                id -> "Fireball",
                id -> "fire"));
        assertTrue(KnownSpellFilters.matches(
                "irons_spellbooks:bless",
                criteria,
                id -> "Bless",
                id -> "holy"));
        assertFalse(KnownSpellFilters.matches(
                "irons_spellbooks:ice_spike",
                criteria,
                id -> "Ice Spike",
                id -> "ice"));
    }

    @Test
    void combinedSearchAndSchoolFiltersOnlyKeepMatchingIntersection() {
        Map<String, String> displayNames = Map.of(
                "irons_spellbooks:fireball", "Fireball",
                "irons_spellbooks:firebolt", "Fire Bolt",
                "irons_spellbooks:flame_wave", "Flame Wave",
                "irons_spellbooks:holy_blast", "Holy Blast"
        );
        Map<String, String> schools = Map.of(
                "irons_spellbooks:fireball", "fire",
                "irons_spellbooks:firebolt", "fire",
                "irons_spellbooks:flame_wave", "fire",
                "irons_spellbooks:holy_blast", "holy"
        );
        KnownSpellFilters.Criteria criteria = new KnownSpellFilters.Criteria("bolt", Set.of("fire"));

        List<String> filtered = displayNames.keySet().stream()
                .filter(id -> KnownSpellFilters.matches(id, criteria, displayNames::get, schools::get))
                .sorted()
                .collect(Collectors.toList());

        assertEquals(List.of("irons_spellbooks:firebolt"), filtered);
    }
}
