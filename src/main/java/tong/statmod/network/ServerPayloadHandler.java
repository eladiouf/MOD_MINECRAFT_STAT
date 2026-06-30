package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tong.statmod.STATMod;
import tong.statmod.integration.ironspells.IronInscriptionOpenerService;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.magic.MagicTreeProgressionService;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.sound.SoundHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleUnlockPerk(UnlockPerkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Perk perk = Perk.byId(payload.perkId());
            if (perk == null) return;

            PlayerStatData data = player.getData(ModAttachments.STATS);
            PerkManager manager = new PerkManager(data);

            if (manager.unlock(perk, player)) {
                PacketDistributor.sendToPlayer(player,
                        new SyncPerksPayload(data.getUnlockedPerks(), data.getPerkPoints()));
                SoundHelper.playPerkUnlock(player);
                STATMod.LOGGER.debug("{} unlocked perk {}", player.getName().getString(), perk.name);
            }
        });
    }

    public static void handleUnlockMagicNode(UnlockMagicNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MagicNode node = MagicTreeCatalog.byId(payload.nodeId());
            if (node == null) return;
            PlayerStatData data = player.getData(ModAttachments.STATS);
            MagicTreeProgressionService.UnlockResult result = MagicTreeProgressionService.tryUnlock(data, node, player);
            if (result.success()) {
                SyncHelper.syncMagic(player);
                STATMod.LOGGER.debug("{} unlocked magic node {}", player.getName().getString(), payload.nodeId());
            } else {
                STATMod.LOGGER.debug("{} failed unlock node {}: {}", player.getName().getString(),
                        payload.nodeId(), result.failure());
            }
        });
    }

    public static void handleOpenPerkTree(OpenPerkTreePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PlayerStatData data = player.getData(ModAttachments.STATS);
            PuffishSkillsCompat.sync(player, data);
            PuffishSkillsCompat.openScreen(player);
        });
    }

    public static void handleOpenVirtualInscription(OpenVirtualInscriptionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            IronInscriptionOpenerService.openVirtual(player);
        });
    }

    /**
     * Demande de changement de branche de départ depuis le Codex du Mage. Validation
     * server-side : la branche doit faire partie des affinités naturelles de la race.
     * Sinon no-op silencieux (le serveur est l'autorité, le client n'a pas accès à la table
     * pour faire la validation).
     */
    public static void handleChangeStartBranch(ChangeStartBranchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            PlayerStatData data = player.getData(ModAttachments.STATS);
            tong.statmod.magic.MagicRace race = data.getMagicRace();
            if (race == null) return; // pas de race choisie → rien à valider contre

            int ord = payload.branchOrdinal();
            tong.statmod.magic.MagicBranch[] all = tong.statmod.magic.MagicBranch.values();
            if (ord < 0 || ord >= all.length) return;
            tong.statmod.magic.MagicBranch target = all[ord];
            if (!race.canChooseStartBranch(target)) {
                STATMod.LOGGER.debug("{} rejected start branch {} (race {} not compatible)",
                        player.getName().getString(), target.id, race.name());
                return;
            }
            data.setChosenStartBranch(target);
            SyncHelper.syncMagic(player);
            STATMod.LOGGER.info("{} switched start branch to {}", player.getName().getString(), target.id);
        });
    }
}
