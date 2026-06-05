package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class StatUpdatePacket {
    private final int[] updates;

    public StatUpdatePacket(int index, int level, int xp) {
        this.updates = new int[]{index, level, xp};
    }

    public StatUpdatePacket(int[] updates) {
        this.updates = NetworkPayloadRules.requireStatUpdateValues(updates);
    }

    public static void encode(StatUpdatePacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.updates);
    }

    public static StatUpdatePacket decode(FriendlyByteBuf buf) {
        return new StatUpdatePacket(buf.readVarIntArray(NetworkPayloadRules.MAX_STAT_UPDATE_VALUES));
    }

    public static void handle(StatUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            for (int i = 0; i < packet.updates.length; i += 3) {
                ClientStatsCache.updateStat(packet.updates[i], packet.updates[i+1], packet.updates[i+2]);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
