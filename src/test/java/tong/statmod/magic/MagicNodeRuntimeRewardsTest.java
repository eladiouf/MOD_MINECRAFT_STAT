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
}
