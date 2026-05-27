package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class FatiguePacket {
    private final float fatigue;

    public FatiguePacket(float fatigue) { this.fatigue = fatigue; }

    public static void encode(FatiguePacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.fatigue);
    }

    public static FatiguePacket decode(FriendlyByteBuf buf) {
        return new FatiguePacket(buf.readFloat());
    }

    public static void handle(FatiguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateFatigue(packet.fatigue));
        ctx.get().setPacketHandled(true);
    }
}
