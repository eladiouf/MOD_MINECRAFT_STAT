package tong.statmod.integration.elementals;

import java.util.EnumSet;

public final class ElementalsMageData {
    private boolean mageAwakened;
    private String starterRaceId = "";
    private final EnumSet<ElementalBranch> starterBranches = EnumSet.noneOf(ElementalBranch.class);
    private final EnumSet<ElementalBranch> unlockedBranches = EnumSet.noneOf(ElementalBranch.class);
    private final EnumSet<ElementalBranch> rewardedRareBranches = EnumSet.noneOf(ElementalBranch.class);
    private final EnumSet<ElementalBranch> freeGrantedRewardBranches = EnumSet.noneOf(ElementalBranch.class);
    private float lastSeenChi;
    private float lastSeenXp;
    private int lastSeenLevel;
    private ElementalBranch lastSeenActiveBranch;

    public boolean mageAwakened() {
        return mageAwakened;
    }

    public void setMageAwakened(boolean value) {
        mageAwakened = value;
    }

    public String starterRaceId() {
        return starterRaceId;
    }

    public void setStarterRaceId(String value) {
        starterRaceId = value == null ? "" : value;
    }

    public EnumSet<ElementalBranch> starterBranches() {
        return starterBranches.isEmpty() ? EnumSet.noneOf(ElementalBranch.class) : EnumSet.copyOf(starterBranches);
    }

    public void setStarterBranches(EnumSet<ElementalBranch> branches) {
        starterBranches.clear();
        if (branches != null) {
            starterBranches.addAll(branches);
        }
    }

    public EnumSet<ElementalBranch> unlockedBranches() {
        return unlockedBranches.isEmpty() ? EnumSet.noneOf(ElementalBranch.class) : EnumSet.copyOf(unlockedBranches);
    }

    public void setUnlockedBranches(EnumSet<ElementalBranch> branches) {
        unlockedBranches.clear();
        if (branches != null) {
            unlockedBranches.addAll(branches);
        }
    }

    public EnumSet<ElementalBranch> rewardedRareBranches() {
        return rewardedRareBranches.isEmpty() ? EnumSet.noneOf(ElementalBranch.class) : EnumSet.copyOf(rewardedRareBranches);
    }

    public void setRewardedRareBranches(EnumSet<ElementalBranch> branches) {
        rewardedRareBranches.clear();
        if (branches != null) {
            rewardedRareBranches.addAll(branches);
        }
    }

    public EnumSet<ElementalBranch> freeGrantedRewardBranches() {
        return freeGrantedRewardBranches.isEmpty() ? EnumSet.noneOf(ElementalBranch.class) : EnumSet.copyOf(freeGrantedRewardBranches);
    }

    public void setFreeGrantedRewardBranches(EnumSet<ElementalBranch> branches) {
        freeGrantedRewardBranches.clear();
        if (branches != null) {
            freeGrantedRewardBranches.addAll(branches);
        }
    }

    public float lastSeenChi() {
        return lastSeenChi;
    }

    public void setLastSeenChi(float value) {
        lastSeenChi = value;
    }

    public float lastSeenXp() {
        return lastSeenXp;
    }

    public void setLastSeenXp(float value) {
        lastSeenXp = value;
    }

    public int lastSeenLevel() {
        return lastSeenLevel;
    }

    public void setLastSeenLevel(int value) {
        lastSeenLevel = Math.max(0, value);
    }

    public ElementalBranch lastSeenActiveBranch() {
        return lastSeenActiveBranch;
    }

    public void setLastSeenActiveBranch(ElementalBranch branch) {
        lastSeenActiveBranch = branch;
    }
}
