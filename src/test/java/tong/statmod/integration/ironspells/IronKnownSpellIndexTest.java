package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IronKnownSpellIndexTest {
    private static final Map<String, String> NAMES = Map.of(
            "irons_spellbooks:fireball", "Fireball",
            "wind_spellbooks:air_blade", "Air Blade",
            "addon:void_shield", "Void Shield");
    private static final Map<String, String> SCHOOLS = Map.of(
            "irons_spellbooks:fireball", "fire",
            "wind_spellbooks:air_blade", "wind",
            "addon:void_shield", "void");

    @Test
    void filtersNativeAndAddonSchoolsWithoutAnAllowlist() {
        Map<String, Integer> learned = Map.of(
                "irons_spellbooks:fireball", 3,
                "wind_spellbooks:air_blade", 2,
                "addon:void_shield", 1);

        List<IronKnownSpellIndex.Entry> visible = IronKnownSpellIndex.visible(
                learned, "blade", Set.of("wind"), NAMES::get, SCHOOLS::get, id -> true);

        assertEquals(List.of(new IronKnownSpellIndex.Entry(
                "wind_spellbooks:air_blade", 2)), visible);
        assertEquals(Set.of("fire", "wind", "void"),
                IronKnownSpellIndex.schools(learned, SCHOOLS::get, id -> true));
    }

    @Test
    void searchAcceptsDisplayNameAndRegistryIdAndSortsDeterministically() {
        Map<String, Integer> learned = Map.of(
                "irons_spellbooks:fireball", 3,
                "wind_spellbooks:air_blade", 2,
                "addon:void_shield", 1,
                "removed:ghost", 9);

        assertEquals(List.of(
                new IronKnownSpellIndex.Entry("wind_spellbooks:air_blade", 2),
                new IronKnownSpellIndex.Entry("irons_spellbooks:fireball", 3),
                new IronKnownSpellIndex.Entry("addon:void_shield", 1)),
                IronKnownSpellIndex.visible(learned, "", Set.of(),
                        NAMES::get, SCHOOLS::get, id -> !id.startsWith("removed:")));
        assertEquals(List.of(new IronKnownSpellIndex.Entry("addon:void_shield", 1)),
                IronKnownSpellIndex.visible(learned, "ADDON:VOID", Set.of(),
                        NAMES::get, SCHOOLS::get, id -> true));
    }

    @Test
    void pagesAreClampedAndImmutable() {
        List<IronKnownSpellIndex.Entry> entries = java.util.stream.IntStream.range(0, 8)
                .mapToObj(index -> new IronKnownSpellIndex.Entry("addon:spell_" + index, 1))
                .toList();

        assertEquals(1, IronKnownSpellIndex.maxPage(entries));
        assertEquals(6, IronKnownSpellIndex.page(entries, -5).size());
        assertEquals(2, IronKnownSpellIndex.page(entries, 99).size());
        assertEquals(7, IronKnownSpellIndex.optionIndexOf(entries, "addon:spell_7"));
        assertThrows(UnsupportedOperationException.class,
                () -> IronKnownSpellIndex.page(entries, 0).add(entries.get(0)));
    }
}
