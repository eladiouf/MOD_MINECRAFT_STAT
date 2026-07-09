package tong.statmod.magic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicNodeRuntimeRewardsTest {
    @Test
    void delegatesOnlyTensuraSkillsToRuntimeSink() {
        List<String> granted = new ArrayList<>();

        MagicNodeRuntimeRewards.GrantSummary summary = MagicNodeRuntimeRewards.apply(
                List.of(
                        "irons_spellbooks:firebolt",
                        "tensura:fire_bolt",
                        "tensura:fire_bolt",
                        "tensura:hellfire"
                ),
                granted::add);

        assertEquals(List.of("tensura:fire_bolt", "tensura:hellfire"), granted);
        assertEquals(2, summary.tensuraGranted());
        assertEquals(1, summary.ignored());
    }

    @Test
    void treatsAlreadyKnownTensuraSkillAsGranted() {
        List<String> attempted = new ArrayList<>();

        MagicNodeRuntimeRewards.GrantSummary summary = MagicNodeRuntimeRewards.apply(
                List.of("tensura:accelerated_thoughts"),
                skillId -> "tensura:thought_acceleration".equals(skillId),
                skillId -> {
                    attempted.add(skillId);
                    return false;
                });

        assertEquals(List.of(), attempted, "Already known skill should not trigger sink");
        assertEquals(1, summary.tensuraGranted());
        assertEquals(0, summary.ignored());
    }
}
