package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tong.statmod.STATMod;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
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
            manager.setFromIds(SyncBus.cachedUnlockedIds(player));

            if (manager.unlock(perk)) {
                SyncBus.cacheUnlockedIds(player, manager.getUnlockedIds());
                PacketDistributor.sendToPlayer(player,
                        new SyncPerksPayload(manager.getUnlockedIds(), data.getPerkPoints()));
                STATMod.LOGGER.debug("{} unlocked perk {}", player.getName().getString(), perk.name);
            }
        });
    }
}
