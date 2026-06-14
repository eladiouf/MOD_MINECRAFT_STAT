package tong.statmod.perks;

public enum PerkTier {
    CORE(10, 1),
    ACTIVE(25, 1),
    SYNERGY(40, 2),
    SITUATIONAL(55, 2),
    MASTERY(75, 2),
    TRANSCENDENCE(95, 2);

    public final int levelRequired;
    public final int cost;

    PerkTier(int levelRequired, int cost) {
        this.levelRequired = levelRequired;
        this.cost = cost;
    }
}
