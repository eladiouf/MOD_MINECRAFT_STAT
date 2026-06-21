package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicCurrency;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicRace;
import tong.statmod.magic.MagicTreeCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class MagicTreeViewModel {
    private MagicTreeViewModel() {}

    public static MagicTreeNodeState nodeState(MagicNode node) {
        if (node == null) return MagicTreeNodeState.LOCKED_SENTINEL;
        if (ClientMagicCache.hasMagicNode(node.id())) return MagicTreeNodeState.UNLOCKED;
        for (String p : node.prerequisites()) {
            if (MagicTreeCatalog.LOCKED_SENTINEL.equals(p)) return MagicTreeNodeState.LOCKED_SENTINEL;
            if (!ClientMagicCache.hasMagicNode(p)) return MagicTreeNodeState.MISSING_PREREQ;
        }
        int adjusted = adjustedCost(node);
        int available = switch (node.currency()) {
            case ARCANE -> ClientMagicCache.getArcanePoints();
            case SCHOOL -> ClientMagicCache.getSchoolPoints(node.branch());
        };
        return available >= adjusted ? MagicTreeNodeState.ELIGIBLE : MagicTreeNodeState.MISSING_POINTS;
    }

    public static int adjustedCost(MagicNode node) {
        if (node == null || node.cost() <= 0 || node.branch() == MagicBranch.COMMON) return node.cost();
        int race = ClientMagicCache.getRaceOrdinal();
        if (race < 0) return node.cost() * 2;
        MagicRace r = MagicRace.values()[race];
        if (!r.hasAffinity(node.branch())) return node.cost() * 2;
        int start = ClientMagicCache.getStartBranchOrdinal();
        if (start >= 0 && start != node.branch().ordinal()) {
            return Math.max(1, (node.cost() + 1) / 2);
        }
        return node.cost();
    }

    public static List<MagicNode> visibleNodes(MagicBranch branch) {
        List<MagicNode> all = MagicTreeCatalog.byBranch(branch);
        List<MagicNode> result = new ArrayList<>(all.size());
        for (MagicNode n : all) {
            if (isVisible(n)) result.add(n);
        }
        return Collections.unmodifiableList(result);
    }

    public static boolean isVisible(MagicNode node) {
        if (node == null) return false;
        if (ClientMagicCache.hasMagicNode(node.id())) return true;
        for (String p : node.prerequisites()) {
            if (MagicTreeCatalog.LOCKED_SENTINEL.equals(p)) return false;
            if (!ClientMagicCache.hasMagicNode(p)) return false;
        }
        return true;
    }
}
