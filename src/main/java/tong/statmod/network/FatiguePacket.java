package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class FatiguePacket {
    private final float fatigue;
    private final int maxFatigue;

    public FatiguePacket(float fatigue) { this(fatigue, 200); }

    public FatiguePacket(float fatigue, int maxFatigue) {
        this.fatigue = fatigue;
        this.maxFatigue = maxFatigue;
    }

    public static void encode(FatiguePacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.fatigue);
        buf.writeInt(packet.maxFatigue);
    }

    public static FatiguePacket decode(FriendlyByteBuf buf) {
        return new FatiguePacket(buf.readFloat(), buf.readInt());
    }

    public static void handle(FatiguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateFatigue(packet.fatigue, packet.maxFatigue));
        ctx.get().setPacketHandled(true);
    }
}
