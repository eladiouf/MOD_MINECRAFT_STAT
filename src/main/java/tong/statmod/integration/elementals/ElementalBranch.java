package tong.statmod.integration.elementals;

public enum ElementalBranch {
    AIR("air", true),
    WATER("water", true),
    EARTH("earth", true),
    FIRE("fire", true),
    LIGHTNING("lightning", false),
    BLOOD("blood", false),
    METAL("metal", false);

    private final String elementalsName;
    private final boolean baseBranch;

    ElementalBranch(String elementalsName, boolean baseBranch) {
        this.elementalsName = elementalsName;
        this.baseBranch = baseBranch;
    }

    public String elementalsName() {
        return elementalsName;
    }

    public boolean isBaseBranch() {
        return baseBranch;
    }
}
