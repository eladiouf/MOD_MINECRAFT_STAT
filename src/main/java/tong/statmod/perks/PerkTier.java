package tong.statmod.perks;

public enum PerkTier {
    CORE(10, 1),
    ACTIVE(25, 1),
    SYNERGY(40, 2),
    SITUATIONAL(55, 1),
    MASTERY(75, 2),
    TRANSCENDENCE(95, 2),
    HYBRID(50, 3);

    public final int requiredStatLevel;
    public final int cost;

    PerkTier(int requiredStatLevel, int cost) {
        this.requiredStatLevel = requiredStatLevel;
        this.cost = cost;
    }
}
