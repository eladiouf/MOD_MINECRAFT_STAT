package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionTest {

    private final ConditionContext ctx = ConditionContext.defaultContext();
    private final PlayerStatData data = new PlayerStatData();

    @Test void statCondition_passes_when_level_meets_min() {
        data.setLevel(StatType.ARCANE_POWER.index, 5);
        assertTrue(new Condition.StatCondition(StatType.ARCANE_POWER, 5).evaluate(data, ctx));
    }

    @Test void statCondition_fails_when_level_below_min() {
        data.setLevel(StatType.ARCANE_POWER.index, 3);
        assertFalse(new Condition.StatCondition(StatType.ARCANE_POWER, 5).evaluate(data, ctx));
    }

    @Test void raceCondition_passes_when_matches() {
        data.setMagicRace(MagicRace.ELF);
        assertTrue(new Condition.RaceCondition(MagicRace.ELF).evaluate(data, ctx));
    }

    @Test void raceCondition_fails_when_different() {
        data.setMagicRace(MagicRace.HUMAN);
        assertFalse(new Condition.RaceCondition(MagicRace.ELF).evaluate(data, ctx));
    }

    @Test void raceCondition_fails_when_null() {
        assertFalse(new Condition.RaceCondition(MagicRace.ELF).evaluate(data, ctx));
    }

    @Test void hasSpellCondition_passes_when_learned() {
        data.learnSpell("irons_spellbooks:firebolt");
        assertTrue(new Condition.HasSpellCondition("irons_spellbooks:firebolt").evaluate(data, ctx));
    }

    @Test void hasSpellCondition_fails_when_not_learned() {
        assertFalse(new Condition.HasSpellCondition("irons_spellbooks:firebolt").evaluate(data, ctx));
    }

    @Test void hasNodeCondition_passes_when_unlocked() {
        data.addMagicNode("fire/opener/ignition");
        assertTrue(new Condition.HasNodeCondition("fire/opener/ignition").evaluate(data, ctx));
    }

    @Test void globalLevelCondition_passes_when_level_high_enough() {
        data.setLevel(StatType.BRUTE_FORCE.index, 10);
        assertTrue(new Condition.GlobalLevelCondition(10).evaluate(data, ctx));
    }

    @Test void and_all_true_returns_true() {
        data.setLevel(StatType.ARCANE_POWER.index, 5);
        data.setLevel(StatType.ERUDITION.index, 3);
        assertTrue(Condition.And.of(
                new Condition.StatCondition(StatType.ARCANE_POWER, 5),
                new Condition.StatCondition(StatType.ERUDITION, 3)
        ).evaluate(data, ctx));
    }

    @Test void and_one_false_returns_false() {
        data.setLevel(StatType.ARCANE_POWER.index, 5);
        data.setLevel(StatType.ERUDITION.index, 2);
        assertFalse(Condition.And.of(
                new Condition.StatCondition(StatType.ARCANE_POWER, 5),
                new Condition.StatCondition(StatType.ERUDITION, 3)
        ).evaluate(data, ctx));
    }

    @Test void or_any_true_returns_true() {
        data.setMagicRace(MagicRace.ELF);
        assertTrue(Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        ).evaluate(data, ctx));
    }

    @Test void or_all_false_returns_false() {
        assertFalse(Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        ).evaluate(data, ctx));
    }

    @Test void nested_and_or_works() {
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 2);
        data.setMagicRace(MagicRace.ELF);
        data.setLevel(StatType.FIRE_AFFINITY.index, 5);
        assertTrue(Condition.Or.of(
                Condition.And.of(
                        new Condition.StatCondition(StatType.ARCANE_POWER, 5),
                        new Condition.StatCondition(StatType.ERUDITION, 3)
                ),
                Condition.And.of(
                        new Condition.RaceCondition(MagicRace.ELF),
                        new Condition.StatCondition(StatType.FIRE_AFFINITY, 5)
                )
        ).evaluate(data, ctx));
    }

    @Test void and_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Condition.And(List.of()));
    }

    @Test void or_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Condition.Or(List.of()));
    }

    @Test void stat_minLevel_negative_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Condition.StatCondition(StatType.ARCANE_POWER, -1));
    }
}
