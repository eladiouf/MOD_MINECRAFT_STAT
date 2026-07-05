package tong.statmod.integration;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceModifierRegistryTest {

    @Test
    void plannedMagicRacesGrantCastingSpeedAndManaPool() {
        assertMagicBonuses("tensura:human_saint");
        assertMagicBonuses("tensura:elf_saint");
        assertMagicBonuses("tensura:kijin");
        assertMagicBonuses("tensura:mystic_oni");
        assertMagicBonuses("tensura:slime");
        assertMagicBonuses("tensura:divine_dragon");
        assertMagicBonuses("tensura:vampire");
        assertMagicBonuses("tensura:arch_daemon");
    }

    @Test
    void humanoidHelpersTreatEvolvedHumansElvesAndDwarvesAsHumanoids() {
        assertTrue(RaceModifierRegistry.isHumanoid("tensura:human"));
        assertTrue(RaceModifierRegistry.isHumanoid("tensura:enlightened_human"));
        assertTrue(RaceModifierRegistry.isHumanoid("tensura:human_saint"));
        assertTrue(RaceModifierRegistry.isHumanoid("tensura:divine_human"));
        assertTrue(RaceModifierRegistry.isHumanoid("tensura:enlightened_elf"));
        assertTrue(RaceModifierRegistry.isHumanoid("tensura:divine_elf"));
        assertTrue(RaceModifierRegistry.isHumanoid("tensura:dwarf_saint"));

        assertFalse(RaceModifierRegistry.isMonster("tensura:human_saint"));
        assertFalse(RaceModifierRegistry.isMonster("tensura:enlightened_elf"));
        assertFalse(RaceModifierRegistry.isMonster("tensura:dwarf_saint"));
        assertTrue(RaceModifierRegistry.isMonster("tensura:orc_lord"));
        assertTrue(RaceModifierRegistry.isMonster("tensura:slime"));
    }

    @Test
    void raceModifiersDoNotDeclareTheSameStatTwiceForOneRace() throws Exception {
        for (Map.Entry<String, RaceData> entry : registeredRaceData().entrySet()) {
            Set<Integer> statIndexes = new HashSet<>();
            for (RaceModifier modifier : entry.getValue().modifiers()) {
                assertTrue(statIndexes.add(modifier.statIndex()),
                        entry.getKey() + " declares duplicate race modifier for stat "
                                + modifier.statIndex());
            }
        }
    }

    @Test
    void vampireLordUsesItsStrongIntimidationBonus() {
        RaceData data = RaceModifierRegistry.get("tensura:vampire_lord");

        assertEquals(6, flatBonusFor(data, StatType.INTIMIDATION.index));
    }

    @Test
    void raceModifierRegistryUsesNamedStatTypesInsteadOfNumericIndexes() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java",
                "tong", "statmod", "integration", "RaceModifierRegistry.java"));

        assertFalse(Pattern.compile("mod\\(\\s*\\d+\\s*,").matcher(source).find(),
                "race modifiers must use StatType names instead of fragile numeric indexes");
        assertTrue(source.contains("mod(StatType."),
                "race modifiers should be declared with named StatType values");
    }

    private static void assertMagicBonuses(String raceId) {
        RaceData data = RaceModifierRegistry.get(raceId);
        assertEquals(1, flatBonusFor(data, 13), raceId + " should grant +1 CASTING_SPEED");
        assertEquals(1, flatBonusFor(data, 14), raceId + " should grant +1 MANA_POOL");
    }

    private static int flatBonusFor(RaceData data, int statIndex) {
        return data.modifiers().stream()
                .filter(modifier -> modifier.statIndex() == statIndex)
                .mapToInt(RaceModifier::flatBonus)
                .findFirst()
                .orElse(0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, RaceData> registeredRaceData() throws Exception {
        Field registry = RaceModifierRegistry.class.getDeclaredField("REGISTRY");
        registry.setAccessible(true);
        return (Map<String, RaceData>) registry.get(null);
    }
}
