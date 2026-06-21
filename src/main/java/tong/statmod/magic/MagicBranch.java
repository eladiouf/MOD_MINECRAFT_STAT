package tong.statmod.magic;

public enum MagicBranch {
    COMMON("common", false),
    FIRE("fire", false),
    WATER("water", false),
    AIR("air", false),
    EARTH("earth", false),
    HOLY("holy", true),
    BLOOD("blood", true),
    ENDER("ender", true),
    EVOCATION("evocation", true),
    ELDRITCH("eldritch", true);

    public final String id;
    public final boolean lateGame;

    MagicBranch(String id, boolean lateGame) {
        this.id = id;
        this.lateGame = lateGame;
    }

    public static MagicBranch byId(String id) {
        if (id == null) return null;
        for (MagicBranch b : values()) {
            if (b.id.equals(id)) return b;
        }
        return null;
    }
}
