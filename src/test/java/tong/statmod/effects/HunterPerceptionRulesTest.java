package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HunterPerceptionRulesTest {
    @Test
    void convertsOnlyFiniteBoundedNormalizedScoresToMilestones() {
        assertEquals(0, HunterPerceptionRules.milestoneCount(Double.NaN));
        assertEquals(0, HunterPerceptionRules.milestoneCount(-1.0));
        assertEquals(1, HunterPerceptionRules.milestoneCount(0.25));
        assertEquals(2, HunterPerceptionRules.milestoneCount(0.50));
        assertEquals(3, HunterPerceptionRules.milestoneCount(0.75));
        assertEquals(3, HunterPerceptionRules.milestoneCount(50.0));
    }

    @Test
    void followsExactTrackingAnchors() {
        assertEquals(0, HunterPerceptionRules.trackingDurationTicks(0, 0.0));
        assertEquals(0.0, HunterPerceptionRules.trackingRangeBlocks(0, 0.0), 1.0e-9);
        assertEquals(61, HunterPerceptionRules.trackingDurationTicks(1, 0.0));
        assertEquals(12.12, HunterPerceptionRules.trackingRangeBlocks(1, 0.0), 1.0e-9);
        assertEquals(125, HunterPerceptionRules.trackingDurationTicks(25, 0.25));
        assertEquals(19.0, HunterPerceptionRules.trackingRangeBlocks(25, 0.25), 1.0e-9);
        assertEquals(190, HunterPerceptionRules.trackingDurationTicks(50, 0.50));
        assertEquals(26.0, HunterPerceptionRules.trackingRangeBlocks(50, 0.50), 1.0e-9);
        assertEquals(255, HunterPerceptionRules.trackingDurationTicks(75, 0.75));
        assertEquals(33.0, HunterPerceptionRules.trackingRangeBlocks(75, 0.75), 1.0e-9);
        assertEquals(280, HunterPerceptionRules.trackingDurationTicks(500, 50.0));
        assertEquals(36.0, HunterPerceptionRules.trackingRangeBlocks(500, 50.0), 1.0e-9);
    }

    @Test
    void followsExactKeenSensesAnchors() {
        assertEquals(0.0, HunterPerceptionRules.keenSensesRangeBlocks(0, 0.0), 1.0e-9);
        assertEquals(6.1, HunterPerceptionRules.keenSensesRangeBlocks(1, 0.0), 1.0e-9);
        assertEquals(10.5, HunterPerceptionRules.keenSensesRangeBlocks(25, 0.25), 1.0e-9);
        assertEquals(15.0, HunterPerceptionRules.keenSensesRangeBlocks(50, 0.50), 1.0e-9);
        assertEquals(19.5, HunterPerceptionRules.keenSensesRangeBlocks(75, 0.75), 1.0e-9);
        assertEquals(22.0, HunterPerceptionRules.keenSensesRangeBlocks(500, 50.0), 1.0e-9);
    }
}
