package tong.statmod.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.ClientStatCache;

@OnlyIn(Dist.CLIENT)
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleSyncPerks(SyncPerksPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPerkCache.update(payload.perkIds(), payload.perStatPoints()));
    }

    public static void handleBatchSync(BatchSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientStatCache.updateAll(payload.levels(), payload.xp(), payload.soulLevel());
            ClientPerkCache.update(payload.perkIds(), payload.perStatPoints());
        });
    }

    public static void handleStatUpdate(StatUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStatCache.updateAll(payload.levels(), payload.xp(), payload.soulLevel()));
    }
}
