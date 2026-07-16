package tong.statmod.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

class AutomaticPerkCatalogTest {
    @Test
    void exposesThirtyThreeStableUniquePerksInPresentationOrder() {
        List<AutomaticPerkDefinition> definitions = AutomaticPerkCatalog.definitions();

        assertEquals(39, definitions.size());
        assertEquals(39, definitions.stream().map(AutomaticPerkDefinition::id)
                .distinct().count());
        assertEquals("statmod:rapidite_25", definitions.get(0).id());
        assertEquals("statmod:magic_resistance_75", definitions.get(20).id());
        assertEquals("statmod:brute_force_25", definitions.get(21).id());
        assertEquals("statmod:physical_resistance_75", definitions.get(32).id());
        assertEquals("statmod:willpower_25", definitions.get(33).id());
        assertEquals("statmod:intimidation_75", definitions.get(38).id());
        assertEquals(definitions.stream().map(AutomaticPerkDefinition::order).sorted().toList(),
                definitions.stream().map(AutomaticPerkDefinition::order).toList());
    }

    @Test
    void excludesEveryRetiredAffinityIdentifier() {
        assertTrue(AutomaticPerkCatalog.byId("statmod:fire_affinity_25").isEmpty());
        assertTrue(AutomaticPerkCatalog.byId("statmod:water_affinity_25").isEmpty());
        assertTrue(AutomaticPerkCatalog.byId("statmod:earth_affinity_25").isEmpty());
        assertTrue(AutomaticPerkCatalog.byId("statmod:air_affinity_25").isEmpty());
    }

    @Test
    void rejectsMalformedDefinitions() {
        assertThrows(IllegalArgumentException.class,
                () -> new AutomaticPerkRequirement(StatType.RAPIDITE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new AutomaticPerkRequirement(StatType.RAPIDITE, 101));
        assertThrows(IllegalArgumentException.class, () -> new AutomaticPerkDefinition(
                "other:bad", 0,
                List.of(new AutomaticPerkRequirement(StatType.RAPIDITE, 25)),
                AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED, 0.02));
        assertThrows(IllegalArgumentException.class, () -> new AutomaticPerkDefinition(
                "statmod:empty", 0, List.of(),
                AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED, 0.02));
        assertThrows(IllegalArgumentException.class, () -> new AutomaticPerkDefinition(
                "statmod:duplicate", 0,
                List.of(new AutomaticPerkRequirement(StatType.RAPIDITE, 25),
                        new AutomaticPerkRequirement(StatType.RAPIDITE, 50)),
                AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED, 0.02));
        assertThrows(IllegalArgumentException.class, () -> new AutomaticPerkDefinition(
                "statmod:nan", 0,
                List.of(new AutomaticPerkRequirement(StatType.RAPIDITE, 25)),
                AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED, Double.NaN));
    }
}
