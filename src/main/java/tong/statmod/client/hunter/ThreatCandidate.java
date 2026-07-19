package tong.statmod.client.hunter;

public record ThreatCandidate(
        int entityId, double distanceSquared, boolean enemy, boolean alive, boolean removed) {
}
