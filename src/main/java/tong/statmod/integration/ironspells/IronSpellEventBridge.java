package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import tong.statmod.STATMod;
import tong.statmod.magic.CastContext;
import tong.statmod.magic.CastRewardPolicy;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicNodeKind;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.magic.SchoolProgressTracker;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class IronSpellEventBridge {
    private IronSpellEventBridge() {}

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        String spellId = IronSpellsApiAdapter.spellId(spell);
        if (shouldCancelPreCast(data, spellId)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("statmod.magic.locked_spell"), true);
        }
    }

    @SubscribeEvent
    public static void onPostCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null) return;
        MagicBranch branch = IronSpellsApiAdapter.branchOf(spell);
        if (branch == null) return;
        String canonicalId = IronSpellsApiAdapter.spellId(spell);

        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        double manaFrac = maxMana > 0 ? event.getManaCost() / maxMana : 0.0;
        boolean hadImpact = manaFrac >= 0.25;
        boolean wasFreeCast = manaFrac <= 0;

        CastContext ctx = new CastContext(canonicalId, branch, manaFrac, hadImpact, wasFreeCast, event.getSpellLevel());
        CastRewardPolicy.Reward reward = CastRewardPolicy.evaluate(ctx);
        if (reward.masteryDelta() > 0) SchoolProgressTracker.applyMastery(data, branch, reward.masteryDelta());
        if (reward.arcaneDelta() > 0) data.addArcanePoints(reward.arcaneDelta());
        STATMod.LOGGER.debug("Cast progression: {} branch={} mastery+={} arcane+={}",
                canonicalId, branch, reward.masteryDelta(), reward.arcaneDelta());
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        MagicBranch branch = IronSpellsApiAdapter.branchOf(event.getSpell());
        int clamped = clampSpellLevel(data, branch, event.getLevel());
        if (clamped < event.getLevel()) event.setLevel(clamped);
    }

    @SubscribeEvent
    public static void onInscribe(InscribeSpellEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        AbstractSpell spell = event.getSpellData().getSpell();
        String spellId = IronSpellsApiAdapter.spellId(spell);
        if (shouldCancelPreCast(data, spellId)) event.setCanceled(true);
    }

    public static boolean shouldCancelPreCast(PlayerStatData data, String spellId) {
        if (data == null || spellId == null) return true;
        return !data.hasLearnedSpell(spellId);
    }

    public static int clampSpellLevel(PlayerStatData data, MagicBranch branch, int requestedLevel) {
        if (data == null || branch == null) return requestedLevel;
        int tierLevel = highestTierReached(data, branch);
        int maxByTier = switch (tierLevel) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            default -> 1;
        };
        return Math.min(requestedLevel, maxByTier);
    }

    private static int highestTierReached(PlayerStatData data, MagicBranch branch) {
        int max = 0;
        for (MagicNode node : MagicTreeCatalog.byBranch(branch)) {
            if (node.kind() == MagicNodeKind.BRANCH_TIER && data.hasMagicNode(node.id())) {
                int t = node.tier().ordinal() + 1;
                if (t > max) max = t;
            }
        }
        return max;
    }
}
