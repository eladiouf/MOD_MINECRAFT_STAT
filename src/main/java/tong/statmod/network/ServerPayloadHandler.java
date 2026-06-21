package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tong.statmod.STATMod;
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
            MagicTreeProgressionService.UnlockResult result = MagicTreeProgressionService.tryUnlock(data, node);
            if (result.success()) {
                PacketDistributor.sendToPlayer(player, new SyncMagicPayload(
                        data.getMagicNodes(), data.getLearnedSpells(),
                        data.getArcanePoints(), data.getSchoolPointsArray(),
                        data.getMagicRace().ordinal(),
                        data.getChosenStartBranch() != null ? data.getChosenStartBranch().ordinal() : -1
                ));
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
}
