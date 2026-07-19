package tong.statmod.perks;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import tong.statmod.stats.StatType;

public final class AutomaticPerkResolver {
    private AutomaticPerkResolver() {
    }

    public static List<AutomaticPerkDefinition> active(Map<StatType, Integer> levels) {
        return active(AutomaticPerkCatalog.definitions(), levels);
    }

    public static List<AutomaticPerkDefinition> active(
            List<AutomaticPerkDefinition> definitions, Map<StatType, Integer> levels) {
        List<AutomaticPerkDefinition> source = definitions == null ? List.of() : definitions;
        Map<StatType, Integer> current = levels == null ? Map.of() : levels;
        return source.stream()
                .filter(definition -> definition.requirements().stream().allMatch(requirement ->
                        current.getOrDefault(requirement.stat(), 0) >= requirement.minimumLevel()))
                .sorted(Comparator.comparingInt(AutomaticPerkDefinition::order))
                .toList();
    }
}
