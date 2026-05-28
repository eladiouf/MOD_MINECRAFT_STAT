package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class ThirstPacket {
    private final float thirst;

    public ThirstPacket(float thirst) { this.thirst = thirst; }

    public static void encode(ThirstPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.thirst);
    }

    public static ThirstPacket decode(FriendlyByteBuf buf) {
        return new ThirstPacket(buf.readFloat());
    }

    public static void handle(ThirstPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateThirst(packet.thirst));
        ctx.get().setPacketHandled(true);
    }
}
