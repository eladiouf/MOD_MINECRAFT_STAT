package tong.statmod.stats;

public enum StatCategory {
    COMBAT("combat"),
    MAGIC("magic"),
    SURVIVAL("survival"),
    CRAFTING("crafting"),
    MENTAL("mental");

    public final String key;

    StatCategory(String key) {
        this.key = key;
    }
}
