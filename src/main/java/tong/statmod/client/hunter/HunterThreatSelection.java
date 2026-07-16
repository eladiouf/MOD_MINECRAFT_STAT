package tong.statmod.client.hunter;

import java.util.Comparator;
import java.util.List;

public final class HunterThreatSelection {
    private static final int HARD_LIMIT = 64;

    private HunterThreatSelection() {
    }

    public static List<Integer> select(List<ThreatCandidate> candidates, double rangeBlocks, int maximum) {
        if (candidates == null || candidates.isEmpty()
                || !Double.isFinite(rangeBlocks) || rangeBlocks <= 0.0D || maximum <= 0) {
            return List.of();
        }
        double rangeSquared = rangeBlocks * rangeBlocks;
        int limit = Math.min(HARD_LIMIT, maximum);
        return candidates.stream()
                .filter(candidate -> candidate != null
                        && candidate.enemy()
                        && candidate.alive()
                        && !candidate.removed()
                        && Double.isFinite(candidate.distanceSquared())
                        && candidate.distanceSquared() >= 0.0D
                        && candidate.distanceSquared() <= rangeSquared)
                .sorted(Comparator.comparingDouble(ThreatCandidate::distanceSquared)
                        .thenComparingInt(ThreatCandidate::entityId))
                .limit(limit)
                .map(ThreatCandidate::entityId)
                .toList();
    }
}
