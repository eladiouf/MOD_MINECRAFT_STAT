package tong.statmod.magic;

import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.ironspells.bridge.TensuraWrapperIds;
import tong.statmod.integration.tensura.TensuraSkillIds;
import tong.statmod.integration.tensura.TensuraSpellTaxonomy;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

public final class MagicTreeProgressionService {
    public record UnlockResult(boolean success,
                                MagicEligibilityResolver.Failure failure,
                                int spent,
                                java.util.List<MagicNodeStatRequirements.StatGate> missingStats) {
        public static UnlockResult ok(int spent) {
            return new UnlockResult(true, MagicEligibilityResolver.Failure.NONE, spent, java.util.List.of());
        }
        public static UnlockResult fail(MagicEligibilityResolver.Failure f) {
            return new UnlockResult(false, f, 0, java.util.List.of());
        }
        public static UnlockResult fail(MagicEligibilityResolver.Result eval) {
            return new UnlockResult(false, eval.failure(), 0, eval.missingStats());
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
            return UnlockResult.fail(eval);
        }
        int adjusted = eval.adjustedCost();
        // Économie unifiée — plus de switch par currency. Mission δ retirera MagicCurrency.
        data.addMagicPoints(-adjusted);
        data.addMagicNode(node.id());
        List<String> newlyLearned = new ArrayList<>();
        List<String> wrapperIdsAdded = new ArrayList<>();
        for (String spell : node.learnedSpells()) {
            if (data.learnSpell(spell)) {
                newlyLearned.add(spell);
                // Pour chaque skill Tensura, ajouter aussi l'ID du wrapper Iron's Spellbooks
                // afin qu'il soit visible dans l'inscription menu (reverse bridge).
                if (spell != null && spell.startsWith("tensura:")) {
                    String canonical = TensuraSkillIds.canonicalize(spell);
                    if (TensuraSpellTaxonomy.profile(canonical) != null) {
                        String wrapperId = TensuraWrapperIds.wrapperIdFor(canonical);
                        if (data.learnSpell(wrapperId)) {
                            wrapperIdsAdded.add(wrapperId);
                        }
                    }
                }
            }
        }
        MagicNodeRuntimeRewards.GrantSummary rewardSummary = MagicNodeRuntimeRewards.apply(newlyLearned, tensuraGrantSink);
        int expectedTensuraRewards = 0;
        for (String spellId : newlyLearned) {
            if (spellId != null && spellId.startsWith("tensura:")) {
                expectedTensuraRewards++;
            }
        }
        if (tensuraGrantSink != null && rewardSummary.tensuraGranted() < expectedTensuraRewards) {
            rollbackUnlock(data, node, adjusted, newlyLearned);
            for (String wrapperId : wrapperIdsAdded) {
                data.forgetSpell(wrapperId);
            }
            return UnlockResult.fail(MagicEligibilityResolver.Failure.RUNTIME_GRANT_FAILED);
        }
        return UnlockResult.ok(adjusted);
    }

    private static void rollbackUnlock(PlayerStatData data, MagicNode node, int adjustedCost, Iterable<String> newlyLearned) {
        if (data == null || node == null) {
            return;
        }
        // Rollback unifié — rend les magicPoints à l'arcane bucket (Mission δ unifiera).
        data.addMagicPoints(adjustedCost);
        data.removeMagicNode(node.id());
        if (newlyLearned == null) {
            return;
        }
        for (String spellId : newlyLearned) {
            data.forgetSpell(spellId);
        }
    }
}
