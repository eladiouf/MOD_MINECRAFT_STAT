package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class ManaSyncPacket {
    private final float mana;

    public ManaSyncPacket(float mana) { this.mana = mana; }

    public static void encode(ManaSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.mana);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buf) {
        return new ManaSyncPacket(buf.readFloat());
    }

    public static void handle(ManaSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateMana(packet.mana));
        ctx.get().setPacketHandled(true);
    }
}
