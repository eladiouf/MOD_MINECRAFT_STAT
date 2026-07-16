package tong.statmod.integration.ironspells;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

public final class IronKnownSpellIndex {
    public static final int PAGE_SIZE = 6;

    private IronKnownSpellIndex() {
    }

    public record Entry(String id, int level) {
    }

    public static List<Entry> visible(
            Map<String, Integer> learned,
            String query,
            Set<String> selectedSchools,
            Function<String, String> displayNameResolver,
            Function<String, String> schoolResolver,
            Predicate<String> registered) {
        if (learned == null || learned.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = normalize(query);
        Set<String> schools = new LinkedHashSet<>();
        if (selectedSchools != null) {
            selectedSchools.stream().map(IronKnownSpellIndex::normalize)
                    .filter(value -> !value.isEmpty()).forEach(schools::add);
        }
        Predicate<String> registryPredicate = registered == null ? id -> true : registered;
        Function<String, String> names = displayNameResolver == null ? id -> id : displayNameResolver;
        Function<String, String> schoolNames = schoolResolver == null ? id -> "" : schoolResolver;

        ArrayList<Entry> result = new ArrayList<>();
        learned.forEach((id, level) -> {
            if (id == null || level == null || level <= 0 || !registryPredicate.test(id)) {
                return;
            }
            String school = normalize(schoolNames.apply(id));
            if (!schools.isEmpty() && !schools.contains(school)) {
                return;
            }
            String name = normalize(names.apply(id));
            if (!normalizedQuery.isEmpty()
                    && !normalize(id).contains(normalizedQuery)
                    && !name.contains(normalizedQuery)) {
                return;
            }
            result.add(new Entry(id, level));
        });
        result.sort(Comparator
                .comparing((Entry entry) -> normalize(names.apply(entry.id())))
                .thenComparing(Entry::id));
        return List.copyOf(result);
    }

    public static Set<String> schools(
            Map<String, Integer> learned,
            Function<String, String> schoolResolver,
            Predicate<String> registered) {
        if (learned == null || learned.isEmpty() || schoolResolver == null) {
            return Set.of();
        }
        Predicate<String> registryPredicate = registered == null ? id -> true : registered;
        TreeSet<String> result = new TreeSet<>();
        learned.forEach((id, level) -> {
            if (id != null && level != null && level > 0 && registryPredicate.test(id)) {
                String school = normalize(schoolResolver.apply(id));
                if (!school.isEmpty()) {
                    result.add(school);
                }
            }
        });
        return Collections.unmodifiableSet(result);
    }

    public static List<Entry> page(List<Entry> entries, int requestedPage) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        int page = Math.max(0, Math.min(maxPage(entries), requestedPage));
        int from = page * PAGE_SIZE;
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        return List.copyOf(entries.subList(from, to));
    }

    public static int maxPage(List<Entry> entries) {
        return entries == null || entries.isEmpty() ? 0 : (entries.size() - 1) / PAGE_SIZE;
    }

    public static int optionIndexOf(List<Entry> entries, String spellId) {
        if (entries == null || spellId == null) {
            return -1;
        }
        for (int index = 0; index < entries.size(); index++) {
            if (spellId.equals(entries.get(index).id())) {
                return index;
            }
        }
        return -1;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
