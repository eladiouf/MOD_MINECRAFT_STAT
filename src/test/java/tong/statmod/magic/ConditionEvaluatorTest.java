package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEvaluatorTest {

    @Test void nullCondition_alwaysPasses() {
        MagicNode node = new MagicNode("common/foundation/arcane_focus",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of(), java.util.Set.of(), null);
        assertTrue(ConditionEvaluator.evaluate(node, new PlayerStatData()));
    }

    @Test void statCondition_failing_showsInDescribeMissing() {
        Condition cond = new Condition.StatCondition(StatType.ARCANE_POWER, 5);
        MagicNode node = node(cond);
        List<String> missing = ConditionEvaluator.describeMissing(node, new PlayerStatData());
        assertTrue(missing.stream().anyMatch(m -> m.contains("Arcane Power") && m.contains("5")));
    }

    @Test void statCondition_passing_returnsEmptyMissing() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(StatType.ARCANE_POWER.index, 10);
        Condition cond = new Condition.StatCondition(StatType.ARCANE_POWER, 5);
        MagicNode node = node(cond);
        assertTrue(ConditionEvaluator.describeMissing(node, data).isEmpty());
    }

    @Test void or_withOnePassing_returnsEmptyMissing() {
        PlayerStatData data = new PlayerStatData();
        data.setMagicRace(MagicRace.ELF);
        Condition cond = Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        );
        MagicNode node = node(cond);
        assertTrue(ConditionEvaluator.describeMissing(node, data).isEmpty());
    }

    @Test void or_withAllFailing_listsBothBranches() {
        Condition cond = Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        );
        MagicNode node = node(cond);
        List<String> missing = ConditionEvaluator.describeMissing(node, new PlayerStatData());
        assertEquals(2, missing.size(), "Both branches should show since all fail");
    }

    private static MagicNode node(Condition cond) {
        return new MagicNode("fire/signature/firebolt",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/ember_path"),
                java.util.Set.of("irons_spellbooks:firebolt"), cond);
    }
}
