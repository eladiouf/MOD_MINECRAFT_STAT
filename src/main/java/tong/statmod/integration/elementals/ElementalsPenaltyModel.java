package tong.statmod.integration.elementals;

import java.util.EnumSet;

public final class ElementalsPenaltyModel {
    private ElementalsPenaltyModel() {}

    public static float damageMultiplier(ElementState state) {
        return state == ElementState.AWAKENED ? 0.80f : 1.0f;
    }

    public static float chiCostMultiplier(ElementState state, boolean rareBranch, int rareOwned) {
        float multiplier = state == ElementState.AWAKENED ? 1.25f : 1.0f;
        if (rareBranch) {
            multiplier += 0.25f;
        }
        if (rareOwned > 1) {
            multiplier += 0.10f;
        }
        return multiplier;
    }

    public static float progressionMultiplier(boolean beastfolk, ElementalBranch activeBranch, EnumSet<ElementalBranch> rareOwned) {
        float multiplier = beastfolk ? 0.85f : 1.0f;
        if (activeBranch == null || rareOwned == null || rareOwned.isEmpty()) {
            return multiplier;
        }
        boolean otherRareOwned = rareOwned.stream().anyMatch(branch -> branch != activeBranch);
        return otherRareOwned ? multiplier * 0.80f : multiplier;
    }

    public static float adjustedChiAfterSpend(float previousChi, float currentChi, ElementState state, boolean rareBranch, int rareOwned) {
        if (previousChi <= currentChi) {
            return currentChi;
        }
        float spent = previousChi - currentChi;
        float extra = spent * (chiCostMultiplier(state, rareBranch, rareOwned) - 1.0f);
        return Math.max(0.0f, currentChi - extra);
    }

    public static float adjustedXpAfterGain(float previousXp,
                                            float currentXp,
                                            boolean beastfolk,
                                            ElementalBranch activeBranch,
                                            EnumSet<ElementalBranch> rareOwned) {
        if (currentXp <= previousXp) {
            return currentXp;
        }
        float gained = currentXp - previousXp;
        float kept = gained * progressionMultiplier(beastfolk, activeBranch, rareOwned);
        return previousXp + kept;
    }
}
