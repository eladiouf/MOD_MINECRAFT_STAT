package tong.statmod.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

class AutomaticPerkResolverTest {
    @Test
    void activatesMilestonesOnlyAtTheirExactBoundaries() {
        assertEquals(List.of(), ids(levels(StatType.RAPIDITE, 24)));
        assertEquals(List.of("statmod:rapidite_25"),
                ids(levels(StatType.RAPIDITE, 25)));
        assertEquals(List.of("statmod:rapidite_25"),
                ids(levels(StatType.RAPIDITE, 49)));
        assertEquals(List.of("statmod:rapidite_25", "statmod:rapidite_50"),
                ids(levels(StatType.RAPIDITE, 50)));
        assertEquals(List.of("statmod:rapidite_25", "statmod:rapidite_50"),
                ids(levels(StatType.RAPIDITE, 74)));
        assertEquals(List.of("statmod:rapidite_25", "statmod:rapidite_50",
                "statmod:rapidite_75"), ids(levels(StatType.RAPIDITE, 75)));
    }

    @Test
    void activatesEveryCombatPerkAtTheSameMilestoneBoundaries() {
        assertMilestones(StatType.BRUTE_FORCE, "brute_force");
        assertMilestones(StatType.BLADE_TECHNIQUE, "blade_technique");
        assertMilestones(StatType.PRECISION, "precision");
        assertMilestones(StatType.PHYSICAL_RESISTANCE, "physical_resistance");
    }

    @Test
    void activatesHunterPerksAtTheSameMilestoneBoundaries() {
        assertMilestones(StatType.TRACKING, "tracking");
        assertMilestones(StatType.KEEN_SENSES, "keen_senses");
    }

    @Test
    void supportsCombinedRequirementsWithoutPersistedUnlockState() {
        AutomaticPerkDefinition combined = new AutomaticPerkDefinition(
                "statmod:combined_test", 0,
                List.of(new AutomaticPerkRequirement(StatType.RAPIDITE, 25),
                        new AutomaticPerkRequirement(StatType.AGILITY, 15)),
                AutomaticPerkEffect.AGILITY_MOVEMENT, 0.02);

        assertEquals(List.of(), AutomaticPerkResolver.active(
                List.of(combined), levels(StatType.RAPIDITE, 24, StatType.AGILITY, 15)));
        assertEquals(List.of(), AutomaticPerkResolver.active(
                List.of(combined), levels(StatType.RAPIDITE, 25, StatType.AGILITY, 14)));
        assertEquals(List.of(combined), AutomaticPerkResolver.active(
                List.of(combined), levels(StatType.RAPIDITE, 25, StatType.AGILITY, 15)));
    }

    private static List<String> ids(Map<StatType, Integer> levels) {
        return AutomaticPerkResolver.active(levels).stream()
                .map(AutomaticPerkDefinition::id)
                .toList();
    }

    private static void assertMilestones(StatType stat, String path) {
        String first = "statmod:" + path + "_25";
        String second = "statmod:" + path + "_50";
        String third = "statmod:" + path + "_75";
        assertEquals(List.of(), ids(levels(stat, 24)));
        assertEquals(List.of(first), ids(levels(stat, 25)));
        assertEquals(List.of(first), ids(levels(stat, 49)));
        assertEquals(List.of(first, second), ids(levels(stat, 50)));
        assertEquals(List.of(first, second), ids(levels(stat, 74)));
        assertEquals(List.of(first, second, third), ids(levels(stat, 75)));
    }

    private static Map<StatType, Integer> levels(StatType stat, int level) {
        return levels(stat, level, null, 0);
    }

    private static Map<StatType, Integer> levels(
            StatType first, int firstLevel, StatType second, int secondLevel) {
        EnumMap<StatType, Integer> levels = new EnumMap<>(StatType.class);
        levels.put(first, firstLevel);
        if (second != null) {
            levels.put(second, secondLevel);
        }
        return levels;
    }
}
