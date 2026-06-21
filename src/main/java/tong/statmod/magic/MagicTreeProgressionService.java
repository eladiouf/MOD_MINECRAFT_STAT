package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

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
        for (String spell : node.learnedSpells()) data.learnSpell(spell);
        return UnlockResult.ok(adjusted);
    }
}
