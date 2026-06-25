package tong.statmod.client.inscription;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public final class KnownSpellFilters {
    private KnownSpellFilters() {}

    public record Criteria(String query, Set<String> schools) {
        public Criteria {
            schools = schools == null ? Set.of() : Set.copyOf(schools);
        }

        String normalizedQuery() {
            return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        }
    }

    public static boolean matches(String spellId,
                                  Criteria criteria,
                                  Function<String, String> displayNameResolver,
                                  Function<String, String> schoolResolver) {
        if (spellId == null || spellId.isBlank()) {
            return false;
        }
        if (criteria == null) {
            return true;
        }
        if (!matchesSchool(criteria.schools(), schoolResolver == null ? null : schoolResolver.apply(spellId))) {
            return false;
        }
        return matchesQuery(spellId, criteria.normalizedQuery(),
                displayNameResolver == null ? null : displayNameResolver.apply(spellId));
    }

    private static boolean matchesSchool(Set<String> schools, String schoolId) {
        if (schools == null || schools.isEmpty()) {
            return true;
        }
        if (schoolId == null || schoolId.isBlank()) {
            return false;
        }
        return schools.contains(schoolId.toLowerCase(Locale.ROOT));
    }

    private static boolean matchesQuery(String spellId, String query, String displayName) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedId = spellId.toLowerCase(Locale.ROOT);
        if (normalizedId.contains(query)) {
            return true;
        }
        return displayName != null && displayName.toLowerCase(Locale.ROOT).contains(query);
    }
}
