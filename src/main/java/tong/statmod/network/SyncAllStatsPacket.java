package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class SyncAllStatsPacket {
    private final int[] levels;
    private final int[] xp;

    public SyncAllStatsPacket(int[] levels, int[] xp) {
        this.levels = NetworkPayloadRules.requireStatArray(levels, "levels");
        this.xp = NetworkPayloadRules.requireStatArray(xp, "xp");
        NetworkPayloadRules.requireMatchingStatArrays(this.levels, this.xp, "sync all stats");
    }

    public static void encode(SyncAllStatsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.levels);
        buf.writeVarIntArray(packet.xp);
    }

    public static SyncAllStatsPacket decode(FriendlyByteBuf buf) {
        return new SyncAllStatsPacket(
            buf.readVarIntArray(NetworkPayloadRules.MAX_STAT_COUNT),
            buf.readVarIntArray(NetworkPayloadRules.MAX_STAT_COUNT)
        );
    }

    public static void handle(SyncAllStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateAll(packet.levels, packet.xp));
        ctx.get().setPacketHandled(true);
    }
}
