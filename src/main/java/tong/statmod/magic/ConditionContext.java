package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

public interface ConditionContext {
    MagicTier highestTierInBranch(PlayerStatData data, MagicBranch branch);

    static ConditionContext defaultContext() {
        return (data, branch) -> {
            MagicTier best = null;
            for (MagicNode node : MagicTreeCatalog.byBranch(branch)) {
                if (data.hasMagicNode(node.id())) {
                    if (best == null || node.tier().compareTo(best) > 0) {
                        best = node.tier();
                    }
                }
            }
            return best;
        };
    }
}
