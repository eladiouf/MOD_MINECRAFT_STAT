package tong.statmod.magic;

import net.minecraft.world.entity.player.Player;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

public final class MagicTreeProgressionService {
    public record UnlockResult(boolean success, MagicEligibilityResolver.Failure failure, int spent) {
        public static UnlockResult ok(int spent) {
            return new UnlockResult(true, MagicEligibilityResolver.Failure.NONE, spent);
        }
        public static UnlockResult fail(MagicEligibilityResolver.Failure f) {
            return new UnlockResult(false, f, 0);
        }
    }

    private MagicTreeProgressionService() {}

    public static UnlockResult tryUnlock(PlayerStatData data, MagicNode node) {
        return tryUnlock(data, node, (MagicNodeRuntimeRewards.TensuraGrantSink) null);
    }

    public static UnlockResult tryUnlock(PlayerStatData data, MagicNode node, Player player) {
        return tryUnlock(data, node, grantedSkill -> {
            MagicNodeRuntimeRewards.GrantSummary summary =
                    MagicNodeRuntimeRewards.apply(player, java.util.List.of(grantedSkill));
            return summary.tensuraGranted() > 0;
        });
    }

    public static UnlockResult tryUnlock(PlayerStatData data, MagicNode node,
                                         MagicNodeRuntimeRewards.TensuraGrantSink tensuraGrantSink) {
        MagicEligibilityResolver.Result eval = MagicEligibilityResolver.evaluate(data, node);
        if (eval.failure() != MagicEligibilityResolver.Failure.NONE) {
            return UnlockResult.fail(eval.failure());
        }
        int adjusted = eval.adjustedCost();
        switch (node.currency()) {
            case ARCANE -> data.addArcanePoints(-adjusted);
            case SCHOOL -> data.addSchoolPoints(node.branch(), -adjusted);
        }
        data.addMagicNode(node.id());
        List<String> newlyLearned = new ArrayList<>();
        for (String spell : node.learnedSpells()) {
            if (data.learnSpell(spell)) {
                newlyLearned.add(spell);
            }
        }
        MagicNodeRuntimeRewards.apply(newlyLearned, tensuraGrantSink);
        return UnlockResult.ok(adjusted);
    }
}
