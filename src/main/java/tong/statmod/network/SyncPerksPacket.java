package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientPerkCache;

import java.util.function.Supplier;

public class SyncPerksPacket {
    private final int[] unlockedPerkIds;
    private final int points;

    public SyncPerksPacket(int[] unlockedPerkIds, int points) {
        this.unlockedPerkIds = unlockedPerkIds;
        this.points = points;
    }

    public static void encode(SyncPerksPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.unlockedPerkIds);
        buf.writeInt(packet.points);
    }

    public static SyncPerksPacket decode(FriendlyByteBuf buf) {
        return new SyncPerksPacket(buf.readVarIntArray(), buf.readInt());
    }

    public static void handle(SyncPerksPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPerkCache.update(packet.unlockedPerkIds, packet.points));
        ctx.get().setPacketHandled(true);
    }
}
