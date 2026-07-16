package tong.statmod.client.hunter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class HunterThreatSelectionTest {
    @Test
    void keepsOnlyNearestEligibleThreatsInsideExactRadius() {
        List<ThreatCandidate> input = List.of(
                new ThreatCandidate(1, 25.0, true, true, false),
                new ThreatCandidate(2, 4.0, true, true, false),
                new ThreatCandidate(3, 1.0, false, true, false),
                new ThreatCandidate(4, 9.0, true, false, false),
                new ThreatCandidate(5, 16.0, true, true, true),
                new ThreatCandidate(6, 36.1, true, true, false));

        assertEquals(List.of(2, 1), HunterThreatSelection.select(input, 6.0, 64));
        assertEquals(List.of(2), HunterThreatSelection.select(input, 6.0, 1));
        assertEquals(List.of(), HunterThreatSelection.select(input, 0.0, 64));
        assertEquals(List.of(), HunterThreatSelection.select(input, Double.NaN, 64));
    }

    @Test
    void breaksDistanceTiesByEntityIdAndHardCapsAtSixtyFour() {
        List<ThreatCandidate> input = java.util.stream.IntStream.rangeClosed(1, 80)
                .mapToObj(id -> new ThreatCandidate(81 - id, 4.0, true, true, false))
                .toList();

        List<Integer> selected = HunterThreatSelection.select(input, 8.0, 1000);

        assertEquals(64, selected.size());
        assertEquals(1, selected.get(0));
        assertEquals(64, selected.get(63));
    }
}
