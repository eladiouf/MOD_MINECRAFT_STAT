package tong.statmod.integration;

public record RaceModifier(int statIndex, int flatBonus, double xpMultiplier) {
    public RaceModifier {
        xpMultiplier = Math.max(0.1, Math.min(xpMultiplier, 5.0));
    }
}
